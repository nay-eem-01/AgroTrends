package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.entities.Author;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.model.request.AuthorCreateRequest;
import com.project.agriculturalblogapplication.repositories.AuthorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthorService {

    private final AuthorRepository authorRepository;

    public void createAuthorUser(AuthorCreateRequest authorCreateRequest, String lang) {
        Author author = new Author();
        author.setDesignation(authorCreateRequest.getProfessionalInfoRequest().getDesignation());
        author.setOccupation(authorCreateRequest.getProfessionalInfoRequest().getOccupation());
        author.setWorkPlaceOrInstitution(authorCreateRequest.getProfessionalInfoRequest().getInstitution());
        author.setSpecialities(authorCreateRequest.getProfessionalInfoRequest().getSpecialities());
        // Both were accepted at sign-up and silently dropped before.
        author.setBio(authorCreateRequest.getProfessionalInfoRequest().getProfessionalStatement());
        author.setProfileImageUrl(authorCreateRequest.getProfessionalInfoRequest().getProfileImageUrl());
        author.setUser(authorCreateRequest.getUser());

        authorRepository.save(author);
    }

    public Author findByUserIdWithException(Long userId){
        return authorRepository.findByUserId(userId).orElseThrow(()->
                new ApplicationException(HttpStatus.NOT_FOUND, ErrorCode.ERROR_AUTHOR_NOT_FOUND));
    }

    /** Publishing requires an author profile; callers without one are refused (403) rather than told "not found". */
    public Author findByUserIdOrForbidden(Long userId, String lang){
        return authorRepository.findByUserId(userId).orElseThrow(()->
                new ApplicationException(HttpStatus.FORBIDDEN, ErrorCode.ERROR_AUTHOR_PROFILE_REQUIRED, lang));
    }

    /** The author profile id of a user, empty for readers without one. */
    public Optional<Long> findAuthorIdByUserId(Long userId) {
        return authorRepository.findByUserId(userId).map(Author::getId);
    }

    public Author save(Author author) {
        return authorRepository.save(author);
    }

    public Author findByIdWithException(Long userId){
        return authorRepository.findById(userId).orElseThrow(()->
                new ApplicationException(HttpStatus.NOT_FOUND, ErrorCode.ERROR_AUTHOR_NOT_FOUND));
    }
}
