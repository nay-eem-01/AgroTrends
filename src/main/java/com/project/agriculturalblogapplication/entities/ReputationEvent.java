package com.project.agriculturalblogapplication.entities;

import com.project.agriculturalblogapplication.constatnt.AppTables.AnswerTable;
import com.project.agriculturalblogapplication.constatnt.AppTables.CategoryTable;
import com.project.agriculturalblogapplication.constatnt.AppTables.QuestionTable;
import com.project.agriculturalblogapplication.constatnt.AppTables.ReputationEventTable;
import com.project.agriculturalblogapplication.constatnt.AppTables.UserTable;
import com.project.agriculturalblogapplication.enums.ReputationReason;
import com.project.agriculturalblogapplication.model.AuditModel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * One line of the append-only reputation ledger: {@code user} gained (or, for a reversal, lost) {@code points} in
 * {@code topic} because of the post it points at. Rows go when that post goes, so deleting a post takes back what
 * it earned.
 */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = ReputationEventTable.NAME, indexes = @Index(columnList = UserTable.USER_ID))
public class ReputationEvent extends AuditModel<String> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = UserTable.USER_ID, nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    /** The question's topic; null for questions asked before topics existed. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = CategoryTable.CATEGORY_ID)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Category topic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = QuestionTable.QUESTION_ID)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = AnswerTable.ANSWER_ID)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Answer answer;

    @Enumerated(EnumType.STRING)
    @Column(name = ReputationEventTable.REASON, nullable = false)
    private ReputationReason reason;

    @Column(name = ReputationEventTable.POINTS, nullable = false)
    private int points;
}
