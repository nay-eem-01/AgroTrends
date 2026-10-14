package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.entities.Author;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.repositories.AuthorRepository;
import com.project.agriculturalblogapplication.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthorService {

    private final AuthorRepository authorRepository;

    private final UserRepository userRepository;

    public Author findByUserIdWithException(Long userId){
        return authorRepository.findByUserId(userId).orElseThrow(()->
                new ApplicationException(HttpStatus.NOT_FOUND, ErrorCode.ERROR_AUTHOR_NOT_FOUND));
    }

    /** The user's profile, created the first time it is needed: every account may publish (PLAN decision 11). */
    public Author profileOf(Long userId) {
        return authorRepository.findByUserId(userId).orElseGet(() -> {
            Author author = new Author();
            // A loaded user, not a lazy reference: responses read the name after the session has closed.
            author.setUser(userRepository.findById(userId).orElseThrow(() ->
                    new ApplicationException(HttpStatus.NOT_FOUND, ErrorCode.ERROR_USER_NOT_FOUND)));
            return authorRepository.save(author);
        });
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
