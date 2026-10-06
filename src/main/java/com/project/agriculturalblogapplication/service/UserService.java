package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.model.response.PublicUserResponse;
import com.project.agriculturalblogapplication.model.response.UserResponse;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.constatnt.AppConstants;
import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.entities.Role;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.model.request.ChangePasswordRequest;
import com.project.agriculturalblogapplication.model.request.UpdateUserRequest;
import com.project.agriculturalblogapplication.payloads.PaginationArgs;
import com.project.agriculturalblogapplication.repositories.UserRepository;
import com.project.agriculturalblogapplication.enums.UserType;
import com.project.agriculturalblogapplication.model.request.AuthorCreateRequest;
import com.project.agriculturalblogapplication.model.request.CreateUserRequest;
import com.project.agriculturalblogapplication.security.service.AttemptLimiter;
import com.project.agriculturalblogapplication.security.service.AuthorizationService;
import com.project.agriculturalblogapplication.security.service.UserSessionService;
import com.project.agriculturalblogapplication.util.AuthUtil;
import com.project.agriculturalblogapplication.util.CommonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

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

        Set<String> userTypeStrings = createUserRequest.getUserTypes()
                .stream()
                .map(Enum::name)
                .collect(Collectors.toSet());

        Role role = roleService.findRoleByNameWithException(AppConstants.USER_ROLE, lang);

        User user = new User();
        user.setName(createUserRequest.getName());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setMobileNumber(mobileNumber);
        user.setRoles(new HashSet<>(Set.of(role)));
        user.setUserTypes(userTypeStrings);

        user = userRepository.save(user);

        User finalUser = user;
        userTypeStrings.forEach(userType -> {
            if (userType.equals(UserType.AUTHOR.name())) {
                AuthorCreateRequest authorCreateRequest = AuthorCreateRequest.builder()
                        .user(finalUser)
                        .professionalInfoRequest(createUserRequest.getProfessionalInfoRequest())
                        .build();

                authorService.createAuthorUser(authorCreateRequest, lang);
            }
        });

        return user;
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
        return UserResponse.from(getUserInfo(lang));
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

    public Page<UserResponse> getAllPaginatedUser(PaginationArgs paginationArgs){
        Pageable pageable = CommonUtils.getPageable(paginationArgs);
        return userRepository.findAll(pageable).map(UserResponse::from);
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

    /** Kills every access session and refresh token of the user. */
    public void revokeAllCredentials(Long userId) {
        userSessionService.deactivatePreviousSession(userId);
        refreshTokenService.deleteAllByUserId(userId);
    }
}
