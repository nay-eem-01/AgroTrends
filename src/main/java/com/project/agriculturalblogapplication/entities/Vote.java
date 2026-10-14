package com.project.agriculturalblogapplication.entities;

import com.project.agriculturalblogapplication.constatnt.AppTables.AnswerTable;
import com.project.agriculturalblogapplication.constatnt.AppTables.QuestionTable;
import com.project.agriculturalblogapplication.constatnt.AppTables.UserTable;
import com.project.agriculturalblogapplication.constatnt.AppTables.VoteTable;
import com.project.agriculturalblogapplication.enums.VoteDirection;
import com.project.agriculturalblogapplication.model.AuditModel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/** One user's vote on one question or one answer: exactly one of {@code question} and {@code answer} is set. */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = VoteTable.NAME, uniqueConstraints = {
        @UniqueConstraint(columnNames = {UserTable.USER_ID, QuestionTable.QUESTION_ID}),
        @UniqueConstraint(columnNames = {UserTable.USER_ID, AnswerTable.ANSWER_ID})})
public class Vote extends AuditModel<String> {

    // ON DELETE CASCADE: deleting a post or a user takes its votes with it.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = UserTable.USER_ID, nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = QuestionTable.QUESTION_ID)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = AnswerTable.ANSWER_ID)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Answer answer;

    @Enumerated(EnumType.STRING)
    @Column(name = VoteTable.DIRECTION, nullable = false)
    private VoteDirection direction;
}
