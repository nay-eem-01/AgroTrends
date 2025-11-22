package com.project.agriculturalblogapplication.entities;


import com.project.agriculturalblogapplication.model.AuditModel;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;


@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Blogs extends AuditModel<String> {

    private String title;
    private String content;
    private String imageUrl;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Categories category;

    @ManyToOne
    @JoinColumn(name = "Author_Id", nullable = false)
    private User author;

    @OneToMany(mappedBy = "blog", cascade = CascadeType.PERSIST,fetch = FetchType.LAZY)
    private List<Comments> comments;

}
