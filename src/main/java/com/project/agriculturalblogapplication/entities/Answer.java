package com.project.agriculturalblogapplication.entities;

import com.project.agriculturalblogapplication.util.SafeHtml;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.project.agriculturalblogapplication.constatnt.AppTables.UserTable;
import com.project.agriculturalblogapplication.constatnt.AppTables.AnswerTable;
import com.project.agriculturalblogapplication.constatnt.AppTables.QuestionTable;
import com.project.agriculturalblogapplication.model.AuditModel;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = AnswerTable.NAME)
public class Answer extends AuditModel<String> {

    @Column(name = AnswerTable.CONTENT, length = 5000)
    private String content;

    @ManyToOne
    @JoinColumn(name = UserTable.USER_ID)
    private User user;

    @ManyToOne
    @JoinColumn(name = QuestionTable.QUESTION_ID)
    private Question question;

    @ManyToOne
    @JoinColumn(name = AnswerTable.ANSWER_ID)
    private Answer parentAnswer;

    @JsonIgnore
    @OneToMany(mappedBy = "parentAnswer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Answer> replies = new ArrayList<>();

    /** Stored HTML is always safe, whichever endpoint wrote it. */
    @PrePersist
    @PreUpdate
    void sanitize() {
        content = SafeHtml.clean(content);
    }
}
