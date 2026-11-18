package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.storage.ImageStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.LinkedHashSet;
import java.util.Set;

/** Removes detached images only after the owning edit commits, and only when no live row still uses them. */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImageCleanup {
    private final ImageStorage storage;
    private final ApplicationEventPublisher events;
    private final JdbcTemplate jdbc;

    public record DetachedImages(Set<String> urls) {}

    /** Call before replacing/removing fields; values may be URLs or rich HTML. No client-supplied delete endpoint. */
    public void consider(String... values) {
        Set<String> urls = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null) continue;
            if (storage.isStored(value)) urls.add(value);
            Jsoup.parse(value).select("img[src]").forEach(img -> {
                if (storage.isStored(img.attr("src"))) urls.add(img.attr("src"));
            });
        }
        if (!urls.isEmpty()) events.publishEvent(new DetachedImages(Set.copyOf(urls)));
    }

    public void questionReplies(Long questionId) {
        jdbc.queryForList("select content from answers where question_id = ?", String.class, questionId)
                .forEach(this::consider);
    }

    public void blogComments(Long blogId) {
        jdbc.queryForList("select comment_content from comment where blog_id = ?", String.class, blogId)
                .forEach(this::consider);
    }

    public void answerReplies(Long answerId) {
        jdbc.queryForList("select content from answers where answer_id = ?", String.class, answerId)
                .forEach(this::consider);
    }

    public void commentReplies(Long commentId) {
        jdbc.queryForList("select comment_content from comment where parent_comment_id = ?", String.class, commentId)
                .forEach(this::consider);
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void afterCommit(DetachedImages event) {
        for (String url : event.urls()) {
            if (!storage.isStored(url)) continue;
            try {
                if (!referenced(url)) storage.delete(url);
            } catch (RuntimeException ex) {
                // The edit has committed. Never report a failed save or log a potentially sensitive URL.
                log.warn("Image cleanup failed for object {}; check references before retrying",
                        url.substring(url.lastIndexOf('/') + 1), ex);
            }
        }
    }

    private boolean referenced(String url) {
        // Include drafts, hidden posts, inline HTML and credential proofs. Reports are historical snapshots.
        // JSON text handles both scalar and PostgreSQL array fields without loading private rows into Java.
        for (String table : new String[]{"authors", "users", "blogs", "questions", "answers", "comment", "expert_applications"}) {
            Boolean found = jdbc.queryForObject("select exists (select 1 from " + table
                    + " r where strpos(to_jsonb(r)::text, ?) > 0)", Boolean.class, url);
            if (Boolean.TRUE.equals(found)) return true;
        }
        return false;
    }
}
