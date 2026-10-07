package com.project.agriculturalblogapplication.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.project.agriculturalblogapplication.constatnt.AppTables.CategoryTable;
import com.project.agriculturalblogapplication.constatnt.AppTables.BlogTable;
import com.project.agriculturalblogapplication.constatnt.AppTables.AuthorTable;
import com.project.agriculturalblogapplication.constatnt.AppTables.TagTable;
import com.project.agriculturalblogapplication.enums.BlogStatus;
import com.project.agriculturalblogapplication.model.AuditModel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = BlogTable.NAME)
public class Blog extends AuditModel<String> {

    @Column(name = BlogTable.TITLE)
    private String title;

    @Column(name = BlogTable.CONTENT, columnDefinition = "TEXT")
    private String content;

    @Column(name = BlogTable.IMAGE_URL)
    private String imageUrl;

    /** Set once at creation from the title and never changed, so shared links keep working. */
    @Column(name = BlogTable.SLUG, unique = true)
    private String slug;

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

    // Eager: a post has at most five tags and every response shows them, including outside a web request.
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = TagTable.BLOG_TAGS,
            joinColumns = @JoinColumn(name = TagTable.BLOG_ID),
            inverseJoinColumns = @JoinColumn(name = TagTable.TAG_ID))
    private Set<Tag> tags = new LinkedHashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "blog", cascade = CascadeType.PERSIST,fetch = FetchType.LAZY)
    private List<Comment> comments;
}
