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
import com.project.agriculturalblogapplication.security.service.GoogleIdTokenVerifier;
import com.project.agriculturalblogapplication.security.service.GoogleIdentity;
import com.project.agriculturalblogapplication.security.service.UserSessionService;
import com.project.agriculturalblogapplication.util.CommonUtils;
import com.project.agriculturalblogapplication.util.PhoneNumbers;
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
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

	private static final int MAX_FAILED_LOGINS_PER_IDENTIFIER = 5;
	private static final int MAX_FAILED_LOGINS_PER_IP = 20;
	private static final Duration LOGIN_LOCK_WINDOW = Duration.ofMinutes(15);

	private final JwtUtil jwtUtil;

	private final RefreshTokenService refreshTokenService;

	private final AuthenticationManager authenticationManager;

	private final UserService userService;

	private final UserSessionService userSessionService;

	private final AttemptLimiter attemptLimiter;

	private final GoogleIdTokenVerifier googleIdTokenVerifier;

	public void validateEmail(ValidateEmailRequest request, String lang) {
		userService.validateEmail(request.getEmail(), lang);
	}

	/** E-mail, phone or both; each one given is checked for format and for an existing account. */
	public UserResponse signUp(SignUpRequest request, String lang) {
		String email = UserService.emailOrNull(request.getEmail());
		String mobileNumber = userService.mobileOrNull(request.getCountryCode(), request.getMobileNumber(), lang);
		if (email == null && mobileNumber == null) {
			throw new ApplicationException(HttpStatus.BAD_REQUEST, ErrorCode.ERROR_EMAIL_OR_PHONE_IS_REQUIRED, lang);
		}
		if (email != null) {
			userService.validateEmail(email, lang);
		}
		if (mobileNumber != null) {
			userService.validateMobile(mobileNumber, lang);
		}

		String invalidPasswordMessage = CommonUtils.getInvalidPasswordMessage(request.getPassword());
		if (invalidPasswordMessage != null) {
			throw new ApplicationException(HttpStatus.BAD_REQUEST, invalidPasswordMessage, lang);
		}

		CreateUserRequest createUserRequest = CreateUserRequest.builder()
				.email(email)
				.name(request.getName())
				.mobileNumber(mobileNumber)
				.password(request.getPassword())
				.build();

		// createUser sends the e-mail link and the SMS code.
		return userService.toResponse(userService.createUser(createUserRequest, lang));
	}

	public WebTokenResponse signIn(SignInRequest request, String lang, String clientIp) {
		return authenticate(request, lang, clientIp, false);
	}

	public WebTokenResponse signInAsAdmin(SignInRequest request, String lang, String clientIp) {
		return authenticate(request, lang, clientIp, true);
	}

	/**
	 * Credentials are always verified first, and every failure (unknown e-mail or phone, wrong password, wrong portal)
	 * produces the same 401, so the endpoint cannot be used to discover accounts or admins.
	 */
	private WebTokenResponse authenticate(SignInRequest request, String lang, String clientIp, boolean adminPortal) {
		String identifier = normaliseIdentifier(request.getIdentifier());
		String identifierKey = "login:id:" + identifier;
		String ipKey = "login:ip:" + clientIp;

		if (attemptLimiter.isBlocked(identifierKey) || attemptLimiter.isBlocked(ipKey)) {
			throw new ApplicationException(HttpStatus.TOO_MANY_REQUESTS, ErrorCode.ERROR_TOO_MANY_ATTEMPTS, lang);
		}

		Authentication authentication;
		try {
			authentication = authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(identifier, request.getPassword()));
		} catch (AuthenticationException e) {
			attemptLimiter.record(identifierKey, MAX_FAILED_LOGINS_PER_IDENTIFIER, LOGIN_LOCK_WINDOW);
			attemptLimiter.record(ipKey, MAX_FAILED_LOGINS_PER_IP, LOGIN_LOCK_WINDOW);
			throw new ApplicationException(HttpStatus.UNAUTHORIZED, ErrorCode.ERROR_INVALID_CREDENTIALS, lang);
		}

		CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
		User user = userService.findByIdWithException(principal.getId(), lang);

		boolean allowed = adminPortal ? isAdmin(user) : isConsumer(user);
		if (!allowed) {
			throw new ApplicationException(HttpStatus.UNAUTHORIZED, ErrorCode.ERROR_INVALID_CREDENTIALS, lang);
		}

		attemptLimiter.reset(identifierKey);
		SecurityContextHolder.getContext().setAuthentication(authentication);

		if (adminPortal) {
			userSessionService.deactivatePreviousSession(user.getId());
		}
		return issueTokens(user, lang);
	}

	/**
	 * Sign in (or up) with Google: the account linked to this Google id; else the account with this e-mail, now
	 * linked; else a new account. Admin accounts are refused with the same 401 as the password sign-in, so the
	 * admin portal stays password-only.
	 */
	public WebTokenResponse signInWithGoogle(GoogleSignInRequest request, String lang) {
		GoogleIdentity google = googleIdTokenVerifier.verify(request.getIdToken(), lang);
		if (!google.emailVerified() || google.email() == null) {
			throw new ApplicationException(HttpStatus.UNAUTHORIZED, ErrorCode.ERROR_GOOGLE_EMAIL_UNVERIFIED, lang);
		}

		User user = userService.findByGoogleSubject(google.subject())
				.or(() -> Optional.ofNullable(userService.findByEmail(google.email())))
				.orElse(null);
		// Checked before linking, so an admin account is never changed by a Google sign-in.
		if (user != null && !isConsumer(user)) {
			throw new ApplicationException(HttpStatus.UNAUTHORIZED, ErrorCode.ERROR_INVALID_CREDENTIALS, lang);
		}
		// Refused before linking: a refused sign-in must not change the account.
		if (user != null && user.getBannedAt() != null) {
			throw new ApplicationException(HttpStatus.FORBIDDEN, ErrorCode.ERROR_ACCOUNT_BANNED, lang);
		}
		if (user == null) {
			user = userService.createGoogleUser(google, lang);
		} else if (user.getGoogleSubject() == null) {
			user = userService.linkGoogle(user, google.subject());
		}
		log.info("User {} signed in with Google", user.getId());
		return issueTokens(user, lang);
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
		return issueTokens(user, lang);
	}

	/** Every way in (password, Google, refresh) ends here, so a ban is enforced once. */
	private WebTokenResponse issueTokens(User user, String lang) {
		if (user.getBannedAt() != null) {
			throw new ApplicationException(HttpStatus.FORBIDDEN, ErrorCode.ERROR_ACCOUNT_BANNED, lang);
		}
		String jwt = jwtUtil.generateAccessToken(String.valueOf(user.getId()));

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

	/** E-mail lower-cased, phone number in E.164; anything else as typed (it will simply not match an account). */
	private static String normaliseIdentifier(String raw) {
		String identifier = raw.strip();
		if (identifier.contains("@")) {
			return identifier.toLowerCase(Locale.ROOT);
		}
		String phone = PhoneNumbers.normalise(identifier);
		return phone != null ? phone : identifier;
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
