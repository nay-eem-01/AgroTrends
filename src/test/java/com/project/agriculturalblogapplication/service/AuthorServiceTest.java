package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.entities.Author;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.repositories.AuthorRepository;
import com.project.agriculturalblogapplication.repositories.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthorServiceTest {

    private final AuthorRepository authors = mock(AuthorRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final AuthorService authorService = new AuthorService(authors, users);

    @Test
    void anExistingProfileIsReturnedAsIs() {
        Author existing = new Author();
        when(authors.findByUserId(7L)).thenReturn(Optional.of(existing));

        assertSame(existing, authorService.profileOf(7L));
        verify(authors, never()).save(any());
    }

    @Test
    void aUserWithoutAProfileGetsOneOnFirstNeed() {
        User user = new User();
        when(authors.findByUserId(7L)).thenReturn(Optional.empty());
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(authors.save(any(Author.class))).thenAnswer(call -> call.getArgument(0));

        assertSame(user, authorService.profileOf(7L).getUser());
    }
}
