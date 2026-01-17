package com.project.agriculturalblogapplication.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.project.agriculturalblogapplication.constatnt.AppTables;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class AuditModel<U> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = AppTables.AuditModelTable.ID)
    private Long id;

    @CreatedBy
    @Column(name = AppTables.AuditModelTable.CREATED_BY)
    protected U createdBy;

    @LastModifiedBy
    @Column(name = AppTables.AuditModelTable.LAST_MODIFIED_BY)
    protected U lastModifiedBy;

    @CreatedDate
    @Column(name = AppTables.AuditModelTable.CREATION_DATE)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
    protected LocalDateTime creationDate;

    @LastModifiedDate
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
    @Column(name = AppTables.AuditModelTable.LAST_MODIFIED_DATE)
    protected LocalDateTime lastModifiedDate;

    @JsonFormat(shape=JsonFormat.Shape.NUMBER, pattern="s")
    public Long getCreationDateTimeStamp() {
        if (creationDate == null) {
            return 0L;
        } else {
            return this.creationDate.toEpochSecond(OffsetDateTime.now().getOffset());
        }
    }

    @JsonFormat(shape=JsonFormat.Shape.NUMBER, pattern="s")
    public Long getLastModifiedDateTimeStamp() {
        if (lastModifiedDate == null) return 0L;
        return this.lastModifiedDate.toEpochSecond(OffsetDateTime.now().getOffset());
    }
}