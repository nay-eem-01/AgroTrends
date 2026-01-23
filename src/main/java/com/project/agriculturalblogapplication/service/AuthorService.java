package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.entities.Author;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.model.request.AuthorCreateRequest;
import com.project.agriculturalblogapplication.repositories.AuthorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

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
        author.setUser(authorCreateRequest.getUser());

        authorRepository.save(author);
    }

    public Author findByUserIdWithException(Long userId){
        return authorRepository.findById(userId).orElseThrow(()->
                new ApplicationException(HttpStatus.NOT_FOUND, ErrorCode.ERROR_AUTHOR_NOT_FOUND));
    }
}
