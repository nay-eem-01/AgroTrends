package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.model.response.PublicUserResponse;
import com.project.agriculturalblogapplication.model.response.UserResponse;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.constatnt.AppConstants;
import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.enums.RoleType;
import com.project.agriculturalblogapplication.entities.Role;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.model.request.ChangePasswordRequest;
import com.project.agriculturalblogapplication.model.request.UpdateUserRequest;
import com.project.agriculturalblogapplication.payloads.PaginationArgs;
import com.project.agriculturalblogapplication.repositories.UserRepository;
import com.project.agriculturalblogapplication.model.request.CreateUserRequest;
import com.project.agriculturalblogapplication.security.service.GoogleIdentity;
import com.project.agriculturalblogapplication.security.service.AttemptLimiter;
import com.project.agriculturalblogapplication.security.service.AuthorizationService;
import com.project.agriculturalblogapplication.security.service.UserSessionService;
import com.project.agriculturalblogapplication.util.AuthUtil;
import com.project.agriculturalblogapplication.util.CommonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
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

    public void validateEmail(String email, String lang) {
        if (!CommonUtils.isValidMail(email)) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, ErrorCode.ERROR_INVALID_EMAIL, lang);
        }

        User existingUser = findByEmail(email);
        if (existingUser != null) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, ErrorCode.ERROR_USER_ALREADY_EXISTS_WITH_EMAIL, lang);
        }
    }

    public void validateMobile(String mobileNumber, String lang) {
        if (existsByMobileNumber(mobileNumber)) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, ErrorCode.ERROR_MOBILE_NUMBER_ALREADY_EXISTS, lang);
        }
    }

    public User createUser(CreateUserRequest createUserRequest, String lang) {
        String email = createUserRequest.getEmail();
        String password = createUserRequest.getPassword();
        String mobileNumber = createUserRequest.getCountryCode().concat(createUserRequest.getMobileNumber());

        Role role = roleService.findRoleByNameWithException(AppConstants.USER_ROLE, lang);

        User user = new User();
        user.setName(createUserRequest.getName());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setMobileNumber(mobileNumber);
        user.setRoles(new HashSet<>(Set.of(role)));
        return userRepository.save(user);
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
        return userRepository.save(user);
    }

    /**
     * Links a Google account to an existing account with the same e-mail. If that e-mail was never verified, whoever
     * registered it may not own it, so its password and every session are dropped: only the Google owner gets in.
     */
    public User linkGoogle(User user, String googleSubject) {
        if (user.getEmailVerifiedAt() == null) {
            user.setPassword(null);
            revokeAllCredentials(user.getId());
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

    public Boolean existsByMobileNumber(String mobileNumber) {
        return userRepository.existsByMobileNumber(mobileNumber);
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

    public Page<UserResponse> getAllPaginatedUser(PaginationArgs paginationArgs, String lang){
        Pageable pageable = CommonUtils.getPageable(paginationArgs, Set.of("creationDate", "name", "email"), lang);
        return userRepository.findAll(pageable).map(this::toResponse);
    }

    public void deleteUser(User user){
        refreshTokenService.deleteAllByUserId(user.getId());
        userSessionService.deleteAllByUserId(user.getId());
        userRepository.delete(user);
    }

    /** Updates the signed-in user's own profile; the target is never taken from the request. */
    public User update(UpdateUserRequest request, String lang){
        User user = findByIdWithException(authorizationService.currentUserId(lang), lang);
        String mobileNumber = request.getCountryCode() + request.getMobileNumber();

        boolean emailChanged = !user.getEmail().equalsIgnoreCase(request.getEmail());
        if (emailChanged) {
            validateEmail(request.getEmail(), lang);
        }
        if (!mobileNumber.equals(user.getMobileNumber())) {
            validateMobile(mobileNumber, lang);
        }

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        if (emailChanged) {
            user.setEmailVerifiedAt(null);
        }
        user.setMobileNumber(mobileNumber);
        user = userRepository.save(user);

        if (emailChanged) {
            // The e-mail is the JWT subject, so existing tokens stop resolving; make that explicit.
            revokeAllCredentials(user.getId());
        }
        return user;
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
    }

    /** Kills every access session and refresh token of the user. */
    public void revokeAllCredentials(Long userId) {
        userSessionService.deactivatePreviousSession(userId);
        refreshTokenService.deleteAllByUserId(userId);
    }
}
