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

    @Column(name = AiAnswerTable.QUESTION)
    private String question;

    @Column(name = AiAnswerTable.AI_ANSWER)
    private String aiAnswer;
}
