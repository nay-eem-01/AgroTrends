package com.project.agriculturalblogapplication.entities;

import com.project.agriculturalblogapplication.util.SafeHtml;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.project.agriculturalblogapplication.model.AuditModel;
import org.hibernate.annotations.ColumnDefault;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class Comment extends AuditModel<String> {

    private String commentContent;

    // ON DELETE CASCADE: deleting a blog takes its comments (and their replies) with it.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blog_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnore
    private Blog blog;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Comment parentComment;

    @OneToMany(
            mappedBy = "parentComment",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @JsonIgnore
    private List<Comment> replies = new ArrayList<>();

    /** Set while enough reports about it await a moderator (roadmap 8.5): invisible to everyone, frozen. */
    @ColumnDefault("false")
    @Column(name = "hidden", nullable = false)
    private boolean hidden;


    /** Stored HTML is always safe, whichever endpoint wrote it. */
    @PrePersist
    @PreUpdate
    void sanitize() {
        commentContent = SafeHtml.clean(commentContent);
    }
}
