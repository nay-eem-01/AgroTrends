package com.project.agriculturalblogapplication.entities;

import com.project.agriculturalblogapplication.constatnt.AppTables.FollowTable;
import com.project.agriculturalblogapplication.model.AuditModel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/** A user following an author: the author's new posts appear in the user's feed. */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = FollowTable.AUTHOR_FOLLOWS, uniqueConstraints = @UniqueConstraint(columnNames = {FollowTable.USER_ID, FollowTable.AUTHOR_ID}))
public class AuthorFollow extends AuditModel<String> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = FollowTable.USER_ID, nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = FollowTable.AUTHOR_ID, nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Author author;
}
