package com.project.agriculturalblogapplication.entities;

import com.project.agriculturalblogapplication.util.SafeHtml;
import com.project.agriculturalblogapplication.constatnt.AppTables.UserTable;
import com.project.agriculturalblogapplication.constatnt.AppTables.AnswerTable;
import com.project.agriculturalblogapplication.constatnt.AppTables.QuestionTable;
import com.project.agriculturalblogapplication.constatnt.AppTables.VoteTable;
import com.project.agriculturalblogapplication.model.AuditModel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
// Postgres does not index foreign keys: these serve the answer list, answerCount and the replies query.
@Table(name = AnswerTable.NAME, indexes = {
        @Index(columnList = QuestionTable.QUESTION_ID),
        @Index(columnList = AnswerTable.ANSWER_ID)})
public class Answer extends AuditModel<String> {

    @Column(name = AnswerTable.CONTENT, length = 5000)
    private String content;

    @ManyToOne
    @JoinColumn(name = UserTable.USER_ID)
    private User user;

    // ON DELETE CASCADE: deleting a question takes its answers, and an answer its replies, in one statement.
    @ManyToOne
    @JoinColumn(name = QuestionTable.QUESTION_ID)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Question question;

    @ManyToOne
    @JoinColumn(name = AnswerTable.ANSWER_ID)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Answer parentAnswer;

    /** Upvotes minus downvotes, kept in step by VoteService so lists need no count query. */
    // Not updatable: saving an edited post must not write back the score it loaded over a vote cast meanwhile.
    @ColumnDefault("0")
    @Column(name = VoteTable.SCORE, nullable = false, updatable = false)
    private long score;

    /** Set while enough reports about it await a moderator (roadmap 8.5): invisible to everyone, frozen. */
    @ColumnDefault("false")
    @Column(name = "hidden", nullable = false)
    private boolean hidden;

    /** Stored HTML is always safe, whichever endpoint wrote it. */
    @PrePersist
    @PreUpdate
    void sanitize() {
        content = SafeHtml.clean(content);
    }
}
