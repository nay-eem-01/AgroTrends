package com.project.agriculturalblogapplication.Models;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Blogs {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long blogId;

    private String title;
    private String content;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;



    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Categories category;

    @ManyToOne
    @JoinColumn(name = "Author_Id", nullable = false)
    private Users author;

    @OneToMany(mappedBy = "blog", cascade = CascadeType.PERSIST,fetch = FetchType.LAZY)
    private List<Comments> comments;

}
