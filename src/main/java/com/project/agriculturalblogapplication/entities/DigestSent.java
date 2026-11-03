package com.project.agriculturalblogapplication.entities;

import com.project.agriculturalblogapplication.model.AuditModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** One weekly digest sent: the unique pair stops a restart (or a second instance) from sending the week twice. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "digest_sent", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "week_start"}))
public class DigestSent extends AuditModel<String> {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    public DigestSent(Long userId, LocalDate weekStart) {
        this.userId = userId;
        this.weekStart = weekStart;
    }
}
