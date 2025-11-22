package com.project.agriculturalblogapplication.entities;

import com.project.agriculturalblogapplication.constatnt.AppTables;
import com.project.agriculturalblogapplication.model.AuditModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = AppTables.PRIVILEGE_NAME)
@Getter
@Setter
@NoArgsConstructor
public class Privilege extends AuditModel<String> {

    @Column(name = AppTables.PrivilegeTable.NAME)
    private String privilegeName;

    @Column(name = AppTables.PrivilegeTable.DESC_NAME)
    private String description;
}
