package com.project.agriculturalblogapplication.entities;

import com.project.agriculturalblogapplication.constatnt.AppTables.AiAnswerTable;
import com.project.agriculturalblogapplication.model.AuditModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@AllArgsConstructor
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

    @Column(name = AiAnswerTable.PROMPT_TOKENS)
    private Integer promptTokens;

    @Column(name = AiAnswerTable.COMPLETION_TOKENS)
    private Integer completionTokens;
}
