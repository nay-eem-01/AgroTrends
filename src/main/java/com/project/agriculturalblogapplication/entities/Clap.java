package com.project.agriculturalblogapplication.entities;

import com.project.agriculturalblogapplication.constatnt.AppTables.ClapTable;
import com.project.agriculturalblogapplication.model.AuditModel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/** One reader's claps on one post (1..50). */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = ClapTable.NAME, uniqueConstraints = @UniqueConstraint(columnNames = {ClapTable.BLOG_ID, ClapTable.USER_ID}))
public class Clap extends AuditModel<String> {

    // ON DELETE CASCADE: deleting a blog or a user takes their claps with them instead of failing on the foreign key.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = ClapTable.BLOG_ID, nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Blog blog;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = ClapTable.USER_ID, nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column(name = ClapTable.CLAP_COUNT, nullable = false)
    private int count;
}
