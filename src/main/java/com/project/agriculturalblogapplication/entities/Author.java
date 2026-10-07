package com.project.agriculturalblogapplication.entities;

import com.project.agriculturalblogapplication.constatnt.AppTables.UserTable;
import com.project.agriculturalblogapplication.constatnt.AppTables.AuthorTable;
import com.project.agriculturalblogapplication.model.AuditModel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = AuthorTable.NAME)
public class Author extends AuditModel<String> {

    @OneToOne
    @JoinColumn(name = UserTable.USER_ID)
    private User user;

    @Column(name = AuthorTable.OCCUPATION)
    private String occupation;

    @Column(name = AuthorTable.DESIGNATION)
    private String designation;

    @Column(name = AuthorTable.WORK_PLACE_OR_INSTITUTION)
    private String workPlaceOrInstitution;

    private List<String> specialities;

    /** The author's own introduction ("professionalStatement" at sign-up). */
    @Column(name = AuthorTable.BIO, columnDefinition = "TEXT")
    private String bio;

    @Column(name = AuthorTable.PROFILE_IMAGE_URL)
    private String profileImageUrl;
}
