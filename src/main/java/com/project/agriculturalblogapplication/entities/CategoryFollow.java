package com.project.agriculturalblogapplication.entities;

import com.project.agriculturalblogapplication.constatnt.AppTables.CategoryTable;
import com.project.agriculturalblogapplication.constatnt.AppTables.FollowTable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Getter
@IdClass(CategoryFollow.Key.class)
@Table(name = FollowTable.CATEGORY_FOLLOWS)
public class CategoryFollow {

    @Id
    @Column(name = FollowTable.USER_ID)
    private Long userId;

    @Id
    @Column(name = CategoryTable.CATEGORY_ID)
    private Long categoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = FollowTable.USER_ID, insertable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = CategoryTable.CATEGORY_ID, insertable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Category category;

    @Column(name = "followed_at", nullable = false)
    private Instant followedAt;

    public record Key(Long userId, Long categoryId) implements Serializable {}
}
