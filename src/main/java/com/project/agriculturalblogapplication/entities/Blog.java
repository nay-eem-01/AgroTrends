package com.project.agriculturalblogapplication.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.project.agriculturalblogapplication.constatnt.AppTables.CategoryTable;
import com.project.agriculturalblogapplication.constatnt.AppTables.BlogTable;
import com.project.agriculturalblogapplication.constatnt.AppTables.AuthorTable;
import com.project.agriculturalblogapplication.enums.BlogStatus;
import com.project.agriculturalblogapplication.model.AuditModel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = BlogTable.NAME)
public class Blog extends AuditModel<String> {

    @Column(name = BlogTable.TITLE)
    private String title;

    @Lob
    @Column(name = BlogTable.CONTENT)
    private String content;

    @Column(name = BlogTable.IMAGE_URL)
    private String imageUrl;

    // Rows that existed before drafts were introduced were all public, so the column defaults to PUBLISHED.
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'PUBLISHED'")
    @Column(name = BlogTable.STATUS, nullable = false)
    private BlogStatus status = BlogStatus.PUBLISHED;

    /** Set the first time the post is published; kept when it is unpublished and published again. */
    @Column(name = BlogTable.PUBLISHED_AT)
    private LocalDateTime publishedAt;

    @ManyToOne
    @JoinColumn(name = CategoryTable.CATEGORY_ID, nullable = false)
    private Category category;

    @ManyToOne
    @JoinColumn(name = AuthorTable.AUTHOR_ID, nullable = false)
    private Author author;

    @JsonIgnore
    @OneToMany(mappedBy = "blog", cascade = CascadeType.PERSIST,fetch = FetchType.LAZY)
    private List<Comment> comments;
}
