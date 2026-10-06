package com.project.agriculturalblogapplication.entities;

import com.project.agriculturalblogapplication.constatnt.AppTables.ClapTable;
import com.project.agriculturalblogapplication.model.AuditModel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One reader's claps on one post (1..50). */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = ClapTable.NAME, uniqueConstraints = @UniqueConstraint(columnNames = {ClapTable.BLOG_ID, ClapTable.USER_ID}))
public class Clap extends AuditModel<String> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = ClapTable.BLOG_ID, nullable = false)
    private Blog blog;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = ClapTable.USER_ID, nullable = false)
    private User user;

    @Column(name = ClapTable.CLAP_COUNT, nullable = false)
    private int count;
}
