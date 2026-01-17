package com.project.agriculturalblogapplication.entities;

import com.project.agriculturalblogapplication.constatnt.AppTables.LanguageTable;
import com.project.agriculturalblogapplication.model.AuditModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor

@Entity
@Table(name = LanguageTable.TABLE_NAME)
public class Language extends AuditModel<String> {

    @Column(name = LanguageTable.LANGUAGE_CODE)
    private String code;

    @Column(name = LanguageTable.NAME)
    private String name;

    @Column(name = LanguageTable.ACTIVE)
    private Boolean active = true;
}
