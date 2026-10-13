package com.project.agriculturalblogapplication.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.project.agriculturalblogapplication.constatnt.AppTables;
import com.project.agriculturalblogapplication.constatnt.AppTables.UserTable;
import com.project.agriculturalblogapplication.model.AuditModel;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;


import java.time.LocalDateTime;
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

    /** BCrypt hash. Null for an account that only signs in with Google and never set a password. */
    @JsonIgnore
    @Column(name = UserTable.PASSWORD)
    private String password;

    @NotBlank
    @Email(message = "Invalid email format")
    @Column(name = UserTable.EMAIL)
    private String email;

    /** Required at e-mail sign-up; null for accounts created through Google, which gives no phone number. */
    @Column(name = UserTable.MOBILE_NUMBER)
    private String mobileNumber;

    /** Google's {@code sub} claim: the stable id of the linked Google account, or null. */
    @Column(name = UserTable.GOOGLE_SUBJECT, unique = true)
    private String googleSubject;

    /** When the user proved they own the e-mail (Google sign-in or a reset link); null until then. */
    @Column(name = UserTable.EMAIL_VERIFIED_AT)
    private LocalDateTime emailVerifiedAt;

    /** Set for seeded accounts; blocks every endpoint except change-password until cleared. Null counts as false. */
    @Column(name = "must_change_password")
    private Boolean mustChangePassword = false;

    @ManyToMany(fetch =  FetchType.EAGER , cascade =  CascadeType.DETACH)
    @JoinTable(
            name = AppTables.USER_ROLE_NAME,
            joinColumns = @JoinColumn(name = AppTables.UserTable.USER_ID),
            inverseJoinColumns = @JoinColumn(name = AppTables.RoleTable.ROLE_ID)
    )
    private Set<Role> roles ;
}

