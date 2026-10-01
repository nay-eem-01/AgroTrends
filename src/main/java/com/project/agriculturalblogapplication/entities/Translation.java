package com.project.agriculturalblogapplication.entities;
import com.project.agriculturalblogapplication.constatnt.AppConstants;
import com.project.agriculturalblogapplication.constatnt.AppTables.TranslationTable;
import com.project.agriculturalblogapplication.model.AuditModel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString

@Entity
@Table(name = TranslationTable.TABLE_NAME)
public class Translation extends AuditModel<String> {
   
    @Column(name = TranslationTable.TRANSLATED_TEXT, columnDefinition = AppConstants.DEFAULT_LARGE_TEXT_DATA_TYPE)
    private String translatedText;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = TranslationTable.LOCALIZED_TEXT_ID)
    private LocalizedText localizedText;

    @Column(name = TranslationTable.LANGUAGE_ID)
    private Long languageId;

    @Column(name = TranslationTable.LANGUAGE_CODE)
    private String languageCode;
}
