package com.project.agriculturalblogapplication.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.project.agriculturalblogapplication.constatnt.AppConstants;
import com.project.agriculturalblogapplication.constatnt.AppTables;
import com.project.agriculturalblogapplication.model.AuditModel;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@ToString
@AllArgsConstructor

@Entity
@Table(name = AppTables.LocalizedTextTable.TABLE_NAME)
public class LocalizedText extends AuditModel<String> {

    @Column(name = AppTables.LocalizedTextTable.ORIGINAL_TEXT, columnDefinition = AppConstants.DEFAULT_LARGE_TEXT_DATA_TYPE)
    private String originalText;

    @Column(name = AppTables.LocalizedTextTable.ORIGINAL_LANGUAGE_CODE)
    private String originalLanguageCode;

    @OneToMany(mappedBy = "localizedText", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    List<Translation> translations = new ArrayList<>();
}
