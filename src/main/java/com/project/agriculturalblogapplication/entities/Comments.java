package com.project.agriculturalblogapplication.entities;

import com.project.agriculturalblogapplication.model.AuditModel;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class Comments extends AuditModel<String> {

    private String commentContent;

    @ManyToOne
    @JoinColumn(name = "blog_id",nullable = false)
    private Blog blog;


    @ManyToOne
    @JoinColumn(name = "user_id",nullable = false)
    private User user;


    @ManyToOne
    @JoinColumn(name = "parent_comment_id")
    private Comments parentComment;

    @OneToMany(
            mappedBy = "parentComment",
            cascade = CascadeType.ALL,
            fetch = FetchType.EAGER,
            orphanRemoval = true
    )
    List<Comments> replies;

}
