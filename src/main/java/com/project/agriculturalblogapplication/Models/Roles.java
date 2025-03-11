package com.project.agriculturalblogapplication.Models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Roles {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long roleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_name")
    private  APP_ROLE role;

}

 enum APP_ROLE {
    ROLE_USER,
    ROLE_ADMIN,
    ROLE_AUTHOR
}