package com.project.agriculturalblogapplication.entities;

import com.project.agriculturalblogapplication.util.SafeHtml;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.project.agriculturalblogapplication.constatnt.AppTables.CategoryTable;
import com.project.agriculturalblogapplication.constatnt.AppTables.QuestionTable;
import com.project.agriculturalblogapplication.constatnt.AppTables.TagTable;
import com.project.agriculturalblogapplication.constatnt.AppTables.UserTable;
import com.project.agriculturalblogapplication.model.AuditModel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = QuestionTable.NAME)
public class Question extends AuditModel<String> {

    @Column(name = QuestionTable.TITLE)
    private String title;

    @Column(name = QuestionTable.CONTENT, length = 5000)
    private String content;

    @ManyToOne
    @JoinColumn(name = UserTable.USER_ID)
    private User user;

    /** The topic; required for new questions, null only on questions asked before topics existed. */
    @ManyToOne
    @JoinColumn(name = CategoryTable.CATEGORY_ID)
    private Category category;

    // Eager for the same reason as Blog.tags: at most five, shown on every response.
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = QuestionTable.QUESTION_TAGS,
            joinColumns = @JoinColumn(name = QuestionTable.QUESTION_ID),
            inverseJoinColumns = @JoinColumn(name = TagTable.TAG_ID))
    private Set<Tag> tags = new LinkedHashSet<>();

    /** Up to four photo URLs from POST /api/images, kept in one array column. */
    // The default fills rows that existed before photos, so the list is never null.
    @JdbcTypeCode(SqlTypes.ARRAY)
    @ColumnDefault("'{}'")
    @Column(name = QuestionTable.IMAGE_URLS, nullable = false)
    private List<String> imageUrls = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Answer> answers = new ArrayList<>();

    @Embedded
    private AgriMetadata agri = new AgriMetadata();

    /** Stored HTML is always safe, whichever endpoint wrote it. */
    @PrePersist
    @PreUpdate
    void sanitize() {
        content = SafeHtml.clean(content);
    }
}
