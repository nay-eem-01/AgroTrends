package com.project.agriculturalblogapplication.model;

import com.project.agriculturalblogapplication.constatnt.AppTables;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
public abstract class AuditModel<U> extends AuditFields<U> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = AppTables.AuditModelTable.ID)
    private Long id;
}
