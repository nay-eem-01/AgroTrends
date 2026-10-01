package com.project.agriculturalblogapplication.entities;

import com.project.agriculturalblogapplication.constatnt.AppTables.RefreshTokenTable;
import com.project.agriculturalblogapplication.model.AuditModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = RefreshTokenTable.TABLE_NAME)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken extends AuditModel<String> {

    @Column(name = RefreshTokenTable.USER_ID)
    private Long userId;

    @Column(name = RefreshTokenTable.TOKEN)
    private String token;

    @Column(name = RefreshTokenTable.EXPIRY_TIME, nullable = false)
    private Instant expiryDate;
}