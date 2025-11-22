package com.project.agriculturalblogapplication.entities;

import com.project.agriculturalblogapplication.constatnt.AppTables;
import com.project.agriculturalblogapplication.enums.RoleType;
import com.project.agriculturalblogapplication.model.AuditModel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collection;

@Entity
@Getter
@Setter
@Table(name = AppTables.ROLE_NAME)
@NoArgsConstructor
public class Role extends AuditModel<String> {

    @Column(name = AppTables.RoleTable.ROLE_NAME)
    private String roleName;

    @Column(name = AppTables.RoleTable.ROLE_TYPE)
    @Enumerated(EnumType.STRING)
    private RoleType roleType;

    @ManyToMany(fetch = FetchType.EAGER , cascade =  CascadeType.DETACH)
    @JoinTable(
            name = AppTables.ROLE_PRIVILEGE_NAME,
            joinColumns = @JoinColumn(name = AppTables.RoleTable.ROLE_ID),
            inverseJoinColumns = @JoinColumn(name = AppTables.PrivilegeTable. PRIVILEGE_ID)
    )
    private Collection<Privilege> privileges;
}
