package com.project.agriculturalblogapplication.entities;

import com.project.agriculturalblogapplication.constatnt.AppTables.CategoryTable;
import com.project.agriculturalblogapplication.constatnt.AppTables.ExpertApplicationTable;
import com.project.agriculturalblogapplication.constatnt.AppTables.UserTable;
import com.project.agriculturalblogapplication.enums.ApplicationStatus;
import com.project.agriculturalblogapplication.enums.CredentialType;
import com.project.agriculturalblogapplication.model.AuditModel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** A user's request to be a Verified Expert in some topics, with the proof an admin checks. */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = ExpertApplicationTable.NAME, indexes = @Index(columnList = UserTable.USER_ID))
public class ExpertApplication extends AuditModel<String> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = UserTable.USER_ID, nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    // Eager: at most three, shown on every response.
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = ExpertApplicationTable.TOPICS,
            joinColumns = @JoinColumn(name = ExpertApplicationTable.EXPERT_APPLICATION_ID),
            inverseJoinColumns = @JoinColumn(name = CategoryTable.CATEGORY_ID))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Set<Category> topics = new LinkedHashSet<>();

    @Column(name = ExpertApplicationTable.FIELD_TITLE, nullable = false, length = 150)
    private String fieldTitle;

    @Enumerated(EnumType.STRING)
    @Column(name = ExpertApplicationTable.CREDENTIAL_TYPE, nullable = false)
    private CredentialType credentialType;

    @Column(name = ExpertApplicationTable.INSTITUTION, nullable = false, length = 150)
    private String institution;

    /** From POST /api/images. */
    // shortcut: uploads are served publicly under an unguessable name; move proofs to private storage if one leaks.
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = ExpertApplicationTable.PROOF_IMAGE_URLS, nullable = false)
    private List<String> proofImageUrls = new ArrayList<>();

    /** Faculty page, staff directory, licence register and the like. */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = ExpertApplicationTable.LINKS, nullable = false)
    private List<String> links = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = ExpertApplicationTable.STATUS, nullable = false)
    private ApplicationStatus status = ApplicationStatus.PENDING;

    /** The admin's reason, shown to the applicant. */
    @Column(name = ExpertApplicationTable.REVIEW_NOTE, length = 500)
    private String reviewNote;
}
