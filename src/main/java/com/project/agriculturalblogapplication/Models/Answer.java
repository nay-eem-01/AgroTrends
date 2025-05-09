package com.project.agriculturalblogapplication.Models;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Answer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 5000)
    private String content;

    @ManyToOne
    private Users user;

    @ManyToOne
    private Question question;

    @ManyToOne
    private Answer parentAnswer;

    @OneToMany(mappedBy = "parentAnswer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Answer> replies = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
