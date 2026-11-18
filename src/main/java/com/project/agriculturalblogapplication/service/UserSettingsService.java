package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.enums.MailKind;
import com.project.agriculturalblogapplication.model.request.UpdateUserSettingsRequest;
import com.project.agriculturalblogapplication.model.response.UserSettingsResponse;
import com.project.agriculturalblogapplication.repositories.UserRepository;
import com.project.agriculturalblogapplication.security.service.AuthorizationService;
import com.project.agriculturalblogapplication.security.service.UnsubscribeTokens;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The caller's e-mail language and e-mail switches, and the unsubscribe links that flip them off. */
@Service
@RequiredArgsConstructor
@Transactional
public class UserSettingsService {

    private final UserService userService;

    private final UserRepository userRepository;

    private final AuthorizationService authorizationService;

    private final UnsubscribeTokens unsubscribeTokens;

    @Transactional(readOnly = true)
    public UserSettingsResponse mine(String lang) {
        return toResponse(caller(lang));
    }

    public UserSettingsResponse update(UpdateUserSettingsRequest request, String lang) {
        User user = caller(lang);
        if (request.preferredLang() != null) {
            user.setPreferredLang(request.preferredLang());
        }
        if (request.emailOnAnswer() != null) {
            user.setEmailOnAnswer(request.emailOnAnswer());
        }
        if (request.emailWeeklyDigest() != null) {
            user.setEmailWeeklyDigest(request.emailWeeklyDigest());
        }
        return toResponse(userRepository.save(user));
    }

    /** Switches one kind of e-mail off for the token's user; false when the token is not valid for that kind. */
    public boolean unsubscribe(String token, MailKind kind) {
        return unsubscribeTokens.userIdOf(token, kind).flatMap(userRepository::findById).map(user -> {
            if (kind == MailKind.answer) {
                user.setEmailOnAnswer(false);
            } else {
                user.setEmailWeeklyDigest(false);
            }
            userRepository.save(user);
            return true;
        }).orElse(false);
    }

    private User caller(String lang) {
        return userService.findByIdWithException(authorizationService.currentUserId(lang), lang);
    }

    private static UserSettingsResponse toResponse(User user) {
        return new UserSettingsResponse(user.getPreferredLang(), user.isEmailOnAnswer(), user.isEmailWeeklyDigest());
    }
}
