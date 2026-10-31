package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.model.response.PublicUserResponse;
import com.project.agriculturalblogapplication.model.response.UserResponse;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.constatnt.AppConstants;
import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.enums.RoleType;
import com.project.agriculturalblogapplication.entities.EmailToken;
import com.project.agriculturalblogapplication.entities.PhoneCode;
import com.project.agriculturalblogapplication.entities.Role;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.model.request.ChangePasswordRequest;
import com.project.agriculturalblogapplication.model.request.UpdateUserRequest;
import com.project.agriculturalblogapplication.repositories.UserRepository;
import com.project.agriculturalblogapplication.model.request.CreateUserRequest;
import com.project.agriculturalblogapplication.security.service.GoogleIdentity;
import com.project.agriculturalblogapplication.security.service.AttemptLimiter;
import com.project.agriculturalblogapplication.security.service.AuthorizationService;
import com.project.agriculturalblogapplication.security.service.UserSessionService;
import com.project.agriculturalblogapplication.util.AuthUtil;
import com.project.agriculturalblogapplication.util.CommonUtils;
import com.project.agriculturalblogapplication.util.PhoneNumbers;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final RoleService roleService;

    private final AuthorService authorService;

    private final AuthorizationService authorizationService;

    private final RefreshTokenService refreshTokenService;

    private final UserSessionService userSessionService;

    private final AttemptLimiter attemptLimiter;

    private final ApplicationEventPublisher eventPublisher;

    private final EmailTokenService emailTokenService;

    private final PhoneCodeService phoneCodeService;

    public void validateEmail(String email, String lang) {
        if (!CommonUtils.isValidMail(email)) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, ErrorCode.ERROR_INVALID_EMAIL, lang);
        }

        User existingUser = findByEmail(email);
        // A banned account's e-mail gets the ban, not "already registered" (it reveals no more).
        if (existingUser != null && existingUser.getBannedAt() != null) {
            throw new ApplicationException(HttpStatus.FORBIDDEN, ErrorCode.ERROR_ACCOUNT_BANNED, lang);
        }
        if (existingUser != null) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, ErrorCode.ERROR_USER_ALREADY_EXISTS_WITH_EMAIL, lang);
        }
    }

    /** {@code mobileNumber} is in E.164 ({@link #mobileOrNull}). */
    public void validateMobile(String mobileNumber, String lang) {
        User existingUser = userRepository.findTopByMobileNumber(mobileNumber).orElse(null);
        if (existingUser != null && existingUser.getBannedAt() != null) {
            throw new ApplicationException(HttpStatus.FORBIDDEN, ErrorCode.ERROR_ACCOUNT_BANNED, lang);
        }
        if (existingUser != null) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, ErrorCode.ERROR_MOBILE_NUMBER_ALREADY_EXISTS, lang);
        }
    }

    /** A trimmed e-mail, or null when none was given. */
    public static String emailOrNull(String email) {
        return StringUtils.hasText(email) ? email.strip() : null;
    }

    /** The number in E.164, or null when none was given; 400 when one was given but is not a phone number. */
    public String mobileOrNull(String countryCode, String mobileNumber, String lang) {
        if (!StringUtils.hasText(mobileNumber)) {
            return null;
        }
        String normalised = PhoneNumbers.normalise(countryCode, mobileNumber);
        if (normalised == null) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, ErrorCode.ERROR_INVALID_MOBILE_NUMBER, lang);
        }
        return normalised;
    }

    public User createUser(CreateUserRequest createUserRequest, String lang) {
        Role role = roleService.findRoleByNameWithException(AppConstants.USER_ROLE, lang);

        User user = new User();
        user.setName(createUserRequest.getName());
        user.setEmail(createUserRequest.getEmail());
        user.setPassword(passwordEncoder.encode(createUserRequest.getPassword()));
        user.setMobileNumber(createUserRequest.getMobileNumber());
        user.setRoles(new HashSet<>(Set.of(role)));
        user = userRepository.save(user);
        // Every user has a profile from the start, so anyone can be followed before they post.
        authorService.profileOf(user.getId());
        // Sends the e-mail link and/or the SMS code.
        eventPublisher.publishEvent(new ContactChangedEvent(user.getId(), user.getEmail() != null,
                user.getMobileNumber() != null));
        return user;
    }

    /**
     * A new account for a Google identity: no password, no mobile number, e-mail already verified by Google.
     */
    public User createGoogleUser(GoogleIdentity google, String lang) {
        Role role = roleService.findRoleByNameWithException(AppConstants.USER_ROLE, lang);

        User user = new User();
        user.setName(StringUtils.hasText(google.name()) ? google.name() : google.email().substring(0, google.email().indexOf('@')));
        user.setEmail(google.email());
        user.setGoogleSubject(google.subject());
        user.setEmailVerifiedAt(LocalDateTime.now());
        user.setRoles(new HashSet<>(Set.of(role)));
        user = userRepository.save(user);
        authorService.profileOf(user.getId());
        return user;
    }

    /**
     * Links a Google account to an existing account with the same e-mail. If that e-mail was never verified, whoever
     * registered it may not own it, so its password, mobile number and every session are dropped: only the Google
     * owner gets in.
     */
    public User linkGoogle(User user, String googleSubject) {
        if (user.getEmailVerifiedAt() == null) {
            user.setPassword(null);
            user.setMobileNumber(null);
            user.setPhoneVerifiedAt(null);
            revokeAllCredentials(user.getId());
            revokeEmailTokens(user.getId());
            revokePhoneCodes(user.getId());
        }
        user.setGoogleSubject(googleSubject);
        user.setEmailVerifiedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    public Optional<User> findByGoogleSubject(String googleSubject) {
        return userRepository.findByGoogleSubject(googleSubject);
    }


    public User findByEmail(String email) {
        return userRepository.findTopByEmailEqualsIgnoreCase(email).orElse(null);
    }

    /** {@code mobileNumber} in E.164. */
    public User findByMobileNumber(String mobileNumber) {
        return userRepository.findTopByMobileNumber(mobileNumber).orElse(null);
    }

    public User findByEmailWithException(String email, String lang) {
        return userRepository.findTopByEmailEqualsIgnoreCase(email).orElseThrow(() ->
                new ApplicationException(HttpStatus.NOT_FOUND, ErrorCode.ERROR_USER_NOT_FOUND, lang));
    }

    public User getUserInfo(String lang) {
        User user = AuthUtil.getLoggedInUser(this);
        if (user == null) {
            throw new ApplicationException(HttpStatus.UNAUTHORIZED, ErrorCode.ERROR_UNAUTHORIZED_ACCESS, lang);
        }

        user = userRepository.findById(user.getId()).orElse(null);
        if (user == null) {
            throw new ApplicationException(HttpStatus.UNAUTHORIZED, ErrorCode.ERROR_UNAUTHORIZED_ACCESS);
        }
        return user;
    }

    public UserResponse getMe(String lang) {
        return toResponse(getUserInfo(lang));
    }

    /** The caller-facing account view, with the author profile id when the user is an author. */
    public UserResponse toResponse(User user) {
        return UserResponse.from(user, authorService.findAuthorIdByUserId(user.getId()).orElse(null));
    }

    public PublicUserResponse getPublicProfile(Long userId, String lang) {
        return PublicUserResponse.from(findByIdWithException(userId, lang));
    }

    public User findByIdWithException(Long userId, String lang) {
        return userRepository.findById(userId).orElseThrow(()->
                new ApplicationException(HttpStatus.NOT_FOUND,ErrorCode.ERROR_USER_NOT_FOUND, lang));
    }

    public User saveUser(User user) {
        return userRepository.save(user);
    }

    public void deleteUser(User user){
        refreshTokenService.deleteAllByUserId(user.getId());
        userSessionService.deleteAllByUserId(user.getId());
        userRepository.delete(user);
    }

    /**
     * Updates the signed-in user's own profile; the target is never taken from the request. E-mail and mobile number
     * can each be added, changed or removed, as long as one remains; a new one starts unconfirmed.
     */
    public User update(UpdateUserRequest request, String lang){
        User user = findByIdWithException(authorizationService.currentUserId(lang), lang);
        String email = emailOrNull(request.getEmail());
        String mobileNumber = mobileOrNull(request.getCountryCode(), request.getMobileNumber(), lang);
        if (email == null && mobileNumber == null) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, ErrorCode.ERROR_EMAIL_OR_PHONE_IS_REQUIRED, lang);
        }

        boolean emailChanged = email == null ? user.getEmail() != null : !email.equalsIgnoreCase(user.getEmail());
        boolean phoneChanged = !Objects.equals(mobileNumber, user.getMobileNumber());
        if (emailChanged && email != null) {
            validateEmail(email, lang);
        }
        if (phoneChanged && mobileNumber != null) {
            validateMobile(mobileNumber, lang);
        }

        user.setName(request.getName());
        // A link or code sent to the old contact must not reset the password or verify the new one.
        if (emailChanged) {
            user.setEmail(email);
            user.setEmailVerifiedAt(null);
            revokeEmailTokens(user.getId());
        }
        if (phoneChanged) {
            user.setMobileNumber(mobileNumber);
            user.setPhoneVerifiedAt(null);
            revokePhoneCodes(user.getId());
        }
        user = userRepository.save(user);

        if ((emailChanged && email != null) || (phoneChanged && mobileNumber != null)) {
            eventPublisher.publishEvent(new ContactChangedEvent(user.getId(), emailChanged && email != null,
                    phoneChanged && mobileNumber != null));
        }
        return user;
    }

    private void revokeEmailTokens(Long userId) {
        for (EmailToken.Purpose purpose : EmailToken.Purpose.values()) {
            emailTokenService.revoke(userId, purpose);
        }
    }

    private void revokePhoneCodes(Long userId) {
        for (PhoneCode.Purpose purpose : PhoneCode.Purpose.values()) {
            phoneCodeService.revoke(userId, purpose);
        }
    }

    public void changePassword(ChangePasswordRequest request, String lang) {
        User user = getUserInfo(lang);
        String attemptKey = "change-password:" + user.getId();

        if (attemptLimiter.isBlocked(attemptKey)) {
            throw new ApplicationException(HttpStatus.TOO_MANY_REQUESTS, ErrorCode.ERROR_TOO_MANY_ATTEMPTS, lang);
        }
        if (user.getPassword() == null) {
            // A Google-only account has no current password to check; "forgot password" sets its first one.
            throw new ApplicationException(HttpStatus.CONFLICT, ErrorCode.ERROR_NO_PASSWORD_SET, lang);
        }
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            attemptLimiter.record(attemptKey, 5, Duration.ofMinutes(15));
            throw new ApplicationException(HttpStatus.BAD_REQUEST, ErrorCode.ERROR_INCORRECT_PASSWORD, lang);
        }
        attemptLimiter.reset(attemptKey);

        if (request.getCurrentPassword().equals(request.getNewPassword())) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, ErrorCode.ERROR_NEW_PASSWORD_SAME_AS_CURRENT, lang);
        }
        setNewPassword(user, request.getNewPassword(), lang);
    }

    /** Applies a new password (policy-checked), clears the must-change flag and signs the user out everywhere. */
    public void setNewPassword(User user, String newPassword, String lang) {
        String invalidPasswordMessage = CommonUtils.getInvalidPasswordMessage(newPassword);
        if (invalidPasswordMessage != null) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, invalidPasswordMessage, lang);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        userRepository.save(user);
        revokeAllCredentials(user.getId());
    }

    /** Suspends an account and signs it out everywhere; admin accounts cannot be banned. */
    @Transactional
    public void ban(Long userId, String reason, String lang) {
        User user = findByIdWithException(userId, lang);
        if (user.getRoles() != null && user.getRoles().stream()
                .anyMatch(role -> role.getRoleType() == RoleType.ADMIN || role.getRoleType() == RoleType.SUPER_ADMIN)) {
            throw new ApplicationException(HttpStatus.FORBIDDEN, ErrorCode.ERROR_CANNOT_BAN_ADMIN, lang);
        }
        user.setBannedAt(LocalDateTime.now());
        user.setBanReason(reason.strip());
        userRepository.save(user);
        revokeAllCredentials(userId);
    }

    @Transactional
    public void unban(Long userId, String lang) {
        User user = findByIdWithException(userId, lang);
        user.setBannedAt(null);
        user.setBanReason(null);
        userRepository.save(user);
        // A session created while the ban was being applied is not revived by the unban.
        revokeAllCredentials(userId);
    }

    /** Kills every access session and refresh token of the user. */
    public void revokeAllCredentials(Long userId) {
        userSessionService.deactivatePreviousSession(userId);
        refreshTokenService.deleteAllByUserId(userId);
    }
}
