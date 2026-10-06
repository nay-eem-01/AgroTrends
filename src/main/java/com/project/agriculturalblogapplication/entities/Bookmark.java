package com.project.agriculturalblogapplication.entities;

import com.project.agriculturalblogapplication.constatnt.AppTables.BookmarkTable;
import com.project.agriculturalblogapplication.model.AuditModel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/** A blog saved to a reader's private reading list. */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = BookmarkTable.NAME, uniqueConstraints = @UniqueConstraint(columnNames = {BookmarkTable.USER_ID, BookmarkTable.BLOG_ID}))
public class Bookmark extends AuditModel<String> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = BookmarkTable.USER_ID, nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = BookmarkTable.BLOG_ID, nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Blog blog;
}
