package com.project.agriculturalblogapplication.entities;

import com.project.agriculturalblogapplication.constatnt.AppTables.TagTable;
import com.project.agriculturalblogapplication.model.AuditModel;
import jakarta.persistence.*;
import lombok.*;

/** A topic such as "rice blast" or "drip irrigation". Names are stored normalised (lowercase, single spaces). */
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = TagTable.NAME)
public class Tag extends AuditModel<String> {

    @Column(name = TagTable.TAG_NAME, unique = true, nullable = false)
    private String name;
}
