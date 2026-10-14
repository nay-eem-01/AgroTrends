package com.project.agriculturalblogapplication.entities;

import com.project.agriculturalblogapplication.constatnt.AppTables.QuestionAiAnswerTable;
import com.project.agriculturalblogapplication.constatnt.AppTables.QuestionTable;
import com.project.agriculturalblogapplication.enums.AiAnswerStatus;
import com.project.agriculturalblogapplication.model.AuditModel;
import com.project.agriculturalblogapplication.model.response.AiSourceResponse;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** The AI's answer to a community question, generated once after the question is saved; labelled until reviewed. */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = QuestionAiAnswerTable.NAME)
public class QuestionAiAnswer extends AuditModel<String> {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = QuestionTable.QUESTION_ID, nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Question question;

    @Column(name = QuestionAiAnswerTable.ANSWER, columnDefinition = "TEXT", nullable = false)
    private String answer;

    /** The posts retrieval used, as cited. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = QuestionAiAnswerTable.SOURCES, nullable = false)
    private List<AiSourceResponse> sources = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = QuestionAiAnswerTable.STATUS, nullable = false)
    private AiAnswerStatus status = AiAnswerStatus.UNCHECKED;

    /** The expert who confirmed, corrected or flagged it; null while unchecked. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = QuestionAiAnswerTable.REVIEWED_BY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private User reviewedBy;

    @Column(name = QuestionAiAnswerTable.REVIEWED_AT)
    private LocalDateTime reviewedAt;

    /** CORRECTED only: the expert's answer posted in its place. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = QuestionAiAnswerTable.CORRECTION_ANSWER_ID)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Answer correction;

    @Column(name = QuestionAiAnswerTable.PROMPT_TOKENS)
    private Integer promptTokens;

    @Column(name = QuestionAiAnswerTable.COMPLETION_TOKENS)
    private Integer completionTokens;
}
