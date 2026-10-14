package com.project.agriculturalblogapplication.entities;

import com.project.agriculturalblogapplication.constatnt.AppTables.AiAnswerTable;
import com.project.agriculturalblogapplication.model.AuditModel;
import com.project.agriculturalblogapplication.model.response.AiSourceResponse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = AiAnswerTable.TABLE_NAME)
public class AiAnswer extends AuditModel<String> {

    @Column(name = AiAnswerTable.USER_ID)
    private Long userId;

    // TEXT, not the default varchar(255): questions may be up to AppConstants.AI_MAX_QUESTION_LENGTH characters.
    @Column(name = AiAnswerTable.QUESTION, columnDefinition = "TEXT")
    private String question;

    @Column(name = AiAnswerTable.AI_ANSWER, columnDefinition = "TEXT")
    private String aiAnswer;

    /** The posts retrieval used; the default fills rows from before sources were kept. */
    @JdbcTypeCode(SqlTypes.JSON)
    @ColumnDefault("'[]'")
    @Column(name = AiAnswerTable.SOURCES, nullable = false)
    private List<AiSourceResponse> sources = new ArrayList<>();

    /** Set once the asker sends this to the experts as a public question (roadmap 7.5). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = AiAnswerTable.PUBLISHED_QUESTION_ID)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Question publishedQuestion;

    @Column(name = AiAnswerTable.PROMPT_TOKENS)
    private Integer promptTokens;

    @Column(name = AiAnswerTable.COMPLETION_TOKENS)
    private Integer completionTokens;
}
