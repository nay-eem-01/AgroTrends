package com.project.agriculturalblogapplication.entities;

import com.project.agriculturalblogapplication.constatnt.AppTables.FollowTable;
import com.project.agriculturalblogapplication.model.AuditModel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/** A user following a topic tag: new posts with that tag appear in the user's feed. */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = FollowTable.TAG_FOLLOWS, uniqueConstraints = @UniqueConstraint(columnNames = {FollowTable.USER_ID, FollowTable.TAG_ID}))
public class TagFollow extends AuditModel<String> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = FollowTable.USER_ID, nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = FollowTable.TAG_ID, nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Tag tag;
}
