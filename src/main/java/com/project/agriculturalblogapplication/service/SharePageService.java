package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.entities.Answer;
import com.project.agriculturalblogapplication.entities.Blog;
import com.project.agriculturalblogapplication.entities.Question;
import com.project.agriculturalblogapplication.enums.AiAnswerStatus;
import com.project.agriculturalblogapplication.enums.BlogStatus;
import com.project.agriculturalblogapplication.repositories.BlogRepositories;
import com.project.agriculturalblogapplication.repositories.QuestionAiAnswerRepository;
import com.project.agriculturalblogapplication.repositories.QuestionRepository;
import lombok.RequiredArgsConstructor;
import com.project.agriculturalblogapplication.util.CommonUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Link-preview pages for Facebook and other crawlers, which do not run the web's JavaScript: Open Graph tags for one
 * question or story, then a script that sends people on to the web page. Anything not public gets the generic card.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SharePageService {

    static final int DESCRIPTION_LENGTH = 200;

    private final QuestionRepository questionRepository;

    private final QuestionAiAnswerRepository questionAiAnswerRepository;

    private final BlogRepositories blogRepositories;

    private final ExpertService expertService;

    @Value("${app.frontendUrl}")
    private String frontendUrl;

    /** A page about a public question, or empty when it is hidden or gone. */
    public Optional<String> question(Long questionId, String shareUrl) {
        return questionRepository.findById(questionId).filter(question -> !question.isHidden()).map(question -> {
            boolean bangla = isBangla(question.getTitle() + question.getContent());
            String text = plainText(question.getContent());
            String description = expertAnswered(question)
                    ? (bangla ? "যাচাইকৃত বিশেষজ্ঞ উত্তর দিয়েছেন · " : "Answered by a verified expert · ") + text
                    : text;
            String image = question.getImageUrls().isEmpty() ? null : question.getImageUrls().get(0);
            return page(question.getTitle(), description, image, bangla, shareUrl, frontendUrl + "/questions/" + question.getId());
        });
    }

    /** A page about a published story, or empty when it is a draft, hidden or gone. */
    public Optional<String> story(String slug, String shareUrl) {
        return blogRepositories.findBySlug(slug).filter(blog -> blog.getStatus() == BlogStatus.PUBLISHED).map((Blog blog) -> {
            String text = blog.getSubtitle() != null && !blog.getSubtitle().isBlank() ? blog.getSubtitle() : plainText(blog.getContent());
            return page(blog.getTitle(), plainText(text), blog.getImageUrl(), isBangla(blog.getTitle() + text), shareUrl,
                    frontendUrl + "/stories/" + blog.getSlug());
        });
    }

    /** The card for anything that cannot be shown: no title, nothing about the item. */
    public String notFound(String shareUrl) {
        return page("AgroTrends", "Questions, answers and stories from Bangladesh's farmers and agriculture experts.",
                null, false, shareUrl, frontendUrl);
    }

    /** Only when it is true: an expert checked the AI answer, or the accepted answer is by an expert in the topic. */
    private boolean expertAnswered(Question question) {
        boolean aiChecked = questionAiAnswerRepository.findByQuestionId(question.getId())
                .map(ai -> ai.getStatus() == AiAnswerStatus.CONFIRMED || ai.getStatus() == AiAnswerStatus.CORRECTED)
                .orElse(false);
        if (aiChecked) {
            return true;
        }
        Answer accepted = question.getAcceptedAnswer();
        if (accepted == null || accepted.isHidden() || question.getCategory() == null) {
            return false;
        }
        Long authorId = accepted.getUser().getId();
        return expertService.verifiedTopics(List.of(authorId)).getOrDefault(authorId, Set.of())
                .contains(question.getCategory().getId());
    }

    private String page(String title, String description, String image, boolean bangla, String shareUrl, String appUrl) {
        String imageUrl = image == null || image.isBlank() ? frontendUrl + "/og-default.png" : image;
        String appUrlJs = appUrl.replace("\\", "\\\\").replace("\"", "\\\"").replace("<", "\\u003c");
        return """
                <!doctype html>
                <html lang="%s">
                <head>
                <meta charset="utf-8">
                <title>%s · AgroTrends</title>
                <meta name="description" content="%s">
                <meta property="og:type" content="article">
                <meta property="og:site_name" content="AgroTrends">
                <meta property="og:title" content="%s">
                <meta property="og:description" content="%s">
                <meta property="og:image" content="%s">
                <meta property="og:url" content="%s">
                <meta property="og:locale" content="%s">
                <meta name="twitter:card" content="summary_large_image">
                <link rel="canonical" href="%s">
                </head>
                <body>
                <p><a href="%s">Open on AgroTrends</a></p>
                <script>location.replace("%s")</script>
                </body>
                </html>
                """.formatted(bangla ? "bn" : "en", attr(title), attr(description), attr(title), attr(description),
                attr(imageUrl), attr(shareUrl), bangla ? "bn_BD" : "en_US", attr(appUrl), attr(appUrl), appUrlJs);
    }

    private static String attr(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value, "UTF-8");
    }

    static String plainText(String html) {
        return CommonUtils.excerpt(html, DESCRIPTION_LENGTH);
    }

    static boolean isBangla(String text) {
        return text != null && text.codePoints().anyMatch(c -> c >= 0x0980 && c <= 0x09FF);
    }
}
