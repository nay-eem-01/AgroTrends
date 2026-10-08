package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.model.response.UserResponse;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.model.request.*;
import com.project.agriculturalblogapplication.entities.RefreshToken;
import com.project.agriculturalblogapplication.security.entites.UserSession;
import com.project.agriculturalblogapplication.security.jwt.JwtUtil;
import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.entities.Role;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.enums.RoleType;
import com.project.agriculturalblogapplication.model.response.WebTokenResponse;
import com.project.agriculturalblogapplication.security.model.request.CreateUserSessionRequest;
import com.project.agriculturalblogapplication.security.service.AttemptLimiter;
import com.project.agriculturalblogapplication.security.service.CustomUserDetails;
import com.project.agriculturalblogapplication.security.service.UserSessionService;
import com.project.agriculturalblogapplication.util.CommonUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

	private static final int MAX_FAILED_LOGINS_PER_EMAIL = 5;
	private static final int MAX_FAILED_LOGINS_PER_IP = 20;
	private static final Duration LOGIN_LOCK_WINDOW = Duration.ofMinutes(15);

	private final JwtUtil jwtUtil;

	private final RefreshTokenService refreshTokenService;

	private final AuthenticationManager authenticationManager;

	private final UserService userService;

	private final UserSessionService userSessionService;

	private final AttemptLimiter attemptLimiter;

	public void validateEmail(ValidateEmailRequest request, String lang) {
		userService.validateEmail(request.getEmail(), lang);
	}

	public UserResponse signUp(SignUpRequest request, String lang) {
		String email = request.getEmail();
		String password = request.getPassword();
		String mobileNumber = request.getCountryCode() + request.getMobileNumber();
		userService.validateEmail(email, lang);
		userService.validateMobile(mobileNumber, lang);

		String invalidPasswordMessage = CommonUtils.getInvalidPasswordMessage(password);
		if (invalidPasswordMessage != null) {
			throw new ApplicationException(HttpStatus.BAD_REQUEST, invalidPasswordMessage, lang);
		}

		CreateUserRequest createUserRequest = CreateUserRequest.builder()
				.email(request.getEmail())
                .name(request.getName())
				.countryCode(request.getCountryCode())
				.mobileNumber(request.getMobileNumber())
				.password(request.getPassword())
                .userTypes(request.getUserType())
                .professionalInfoRequest(request.getProfessionalInfoRequest())
				.build();

		return userService.toResponse(userService.createUser(createUserRequest, lang));
	}

	public WebTokenResponse signIn(SignInRequest request, String lang, String clientIp) {
		return authenticate(request, lang, clientIp, false);
	}

	public WebTokenResponse signInAsAdmin(SignInRequest request, String lang, String clientIp) {
		return authenticate(request, lang, clientIp, true);
	}

	/**
	 * Credentials are always verified first, and every failure (unknown e-mail, wrong password, wrong portal)
	 * produces the same 401, so the endpoint cannot be used to discover accounts or admins.
	 */
	private WebTokenResponse authenticate(SignInRequest request, String lang, String clientIp, boolean adminPortal) {
		String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
		String emailKey = "login:email:" + email;
		String ipKey = "login:ip:" + clientIp;

		if (attemptLimiter.isBlocked(emailKey) || attemptLimiter.isBlocked(ipKey)) {
			throw new ApplicationException(HttpStatus.TOO_MANY_REQUESTS, ErrorCode.ERROR_TOO_MANY_ATTEMPTS, lang);
		}

		Authentication authentication;
		try {
			authentication = authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(email, request.getPassword()));
		} catch (AuthenticationException e) {
			attemptLimiter.record(emailKey, MAX_FAILED_LOGINS_PER_EMAIL, LOGIN_LOCK_WINDOW);
			attemptLimiter.record(ipKey, MAX_FAILED_LOGINS_PER_IP, LOGIN_LOCK_WINDOW);
			throw new ApplicationException(HttpStatus.UNAUTHORIZED, ErrorCode.ERROR_INVALID_CREDENTIALS, lang);
		}

		CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
		User user = userService.findByIdWithException(principal.getId(), lang);

		boolean allowed = adminPortal ? isAdmin(user) : isConsumer(user);
		if (!allowed) {
			throw new ApplicationException(HttpStatus.UNAUTHORIZED, ErrorCode.ERROR_INVALID_CREDENTIALS, lang);
		}

		attemptLimiter.reset(emailKey);
		SecurityContextHolder.getContext().setAuthentication(authentication);

		if (adminPortal) {
			userSessionService.deactivatePreviousSession(user.getId());
		}
		return issueTokens(user);
	}

	/**
	 * Exchanges a refresh token for a new access token and a new refresh token. The presented token is consumed
	 * atomically, so a token can be used exactly once.
	 */
	public WebTokenResponse refreshToken(RefreshTokenRequest request, String lang) {
		RefreshToken stored = refreshTokenService.findByToken(request.getRefreshToken()).orElseThrow(() ->
				new ApplicationException(HttpStatus.UNAUTHORIZED, ErrorCode.ERROR_INVALID_TOKEN, lang));

		refreshTokenService.verifyExpiration(stored, lang);

		if (!refreshTokenService.consume(stored)) {
			throw new ApplicationException(HttpStatus.UNAUTHORIZED, ErrorCode.ERROR_INVALID_TOKEN, lang);
		}

		User user = userService.findByIdWithException(stored.getUserId(), lang);
		userSessionService.deactivateExpiredSessions(user.getId());
		return issueTokens(user);
	}

	private WebTokenResponse issueTokens(User user) {
		String jwt = jwtUtil.generateAccessToken(user.getEmail());

		CreateUserSessionRequest createUserSessionRequest = CreateUserSessionRequest.builder()
				.token(jwt)
				.tokenType("Bearer")
				.platformType(null)
				.userDeviceId(null)
				.userType(isAdmin(user) ? "AUTHORITY" : "CUSTOMER")
				.userId(user.getId())
				.build();
		userSessionService.createNewSession(createUserSessionRequest);

		String refreshToken = refreshTokenService.createRefreshToken(user.getId()).getToken();
		return new WebTokenResponse(jwt, refreshToken, "Bearer", userService.toResponse(user));
	}

	public Boolean isAdmin(User user) {
		for (Role role : user.getRoles()) {
			if (role.getRoleType().equals(RoleType.ADMIN) || role.getRoleType().equals(RoleType.SUPER_ADMIN)) {
				return true;
			}
		}

		return false;
	}

	public Boolean isConsumer(User user) {
		for (Role role : user.getRoles()) {
			if (role.getRoleType().equals(RoleType.USER)) {
				return true;
			}
		}

		return false;
	}

	public void signOut(HttpServletRequest httpServletRequest, String lang) {
		User user = userService.getUserInfo(lang);

		String bearerToken = httpServletRequest.getHeader("Authorization");
		if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
			UserSession userSession = userSessionService.getActiveSessionByToken(bearerToken.substring(7));
			userSessionService.deactivateSession(userSession);
		}

		refreshTokenService.deleteAllByUserId(user.getId());
	}
}
