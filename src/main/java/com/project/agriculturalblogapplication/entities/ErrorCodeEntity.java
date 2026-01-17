package com.project.agriculturalblogapplication.entities;

import com.project.agriculturalblogapplication.constatnt.AppTables;
import com.project.agriculturalblogapplication.model.AuditModel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = AppTables.ErrorCodeEntityTable.TABLE_NAME)
public class ErrorCodeEntity extends AuditModel<String> {

    @Column(name = AppTables.ErrorCodeEntityTable.INTERNAL_CODE)
    private String internalCode;

    @Column(name = AppTables.ErrorCodeEntityTable.INTERNAL_MESSAGE)
    private String internalMessage;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = AppTables.ErrorCodeEntityTable.MESSAGE)
    private LocalizedText message;
}
