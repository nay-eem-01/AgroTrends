package com.project.agriculturalblogapplication.entities;


import com.project.agriculturalblogapplication.constatnt.AppTables.CategoryTable;
import com.project.agriculturalblogapplication.constatnt.AppTables.BlogTable;
import com.project.agriculturalblogapplication.constatnt.AppTables.AuthorTable;
import com.project.agriculturalblogapplication.model.AuditModel;
import jakarta.persistence.*;
import lombok.*;

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

    @Column(name = BlogTable.CONTENT)
    private String content;

    @Column(name = BlogTable.IMAGE_URL)
    private String imageUrl;

    @ManyToOne
    @JoinColumn(name = CategoryTable.CATEGORY_ID, nullable = false)
    private Categories category;

    @ManyToOne
    @JoinColumn(name = AuthorTable.AUTHOR_ID, nullable = false)
    private Author author;

    @OneToMany(mappedBy = "blog", cascade = CascadeType.PERSIST,fetch = FetchType.LAZY)
    private List<Comments> comments;

}
