package com.project.agriculturalblogapplication.entities;

import com.project.agriculturalblogapplication.constatnt.AppTables;
import com.project.agriculturalblogapplication.constatnt.AppTables.UserTable;
import com.project.agriculturalblogapplication.enums.UserType;
import com.project.agriculturalblogapplication.model.AuditModel;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;


import java.util.Set;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = AppTables.USER_NAME)
public class User extends AuditModel<String> {

    @NotBlank(message = "Name can't be blank")
    @Column(name = UserTable.NAME)
    private String name;

    @NotBlank(message = "Password can't be blank")
    @Size(min = 4,message = "Password should contain at least 4 character")
    @Column(name = UserTable.PASSWORD)
    private String password;

    @NotBlank
    @Email(message = "Invalid email format")
    @Column(name = UserTable.EMAIL)
    private String email;

    @Column(name = AppTables.USER_TYPE)
    private UserType userType;

    @ManyToMany(fetch =  FetchType.EAGER , cascade =  CascadeType.DETACH)
    @JoinTable(
            name = AppTables.USER_ROLE_NAME,
            joinColumns = @JoinColumn(name = AppTables.UserTable.USER_ID),
            inverseJoinColumns = @JoinColumn(name = AppTables.RoleTable.ROLE_ID)
    )
    private Set<Role> roles ;
}

