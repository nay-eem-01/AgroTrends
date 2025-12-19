package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.model.request.*;
import com.project.agriculturalblogapplication.security.entites.UserSession;
import com.project.agriculturalblogapplication.security.jwt.JwtUtil;
import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.entities.Role;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.enums.RoleType;
import com.project.agriculturalblogapplication.model.response.WebTokenResponse;
import com.project.agriculturalblogapplication.security.model.request.CreateUserSessionRequest;
import com.project.agriculturalblogapplication.security.service.UserSessionService;
import com.project.agriculturalblogapplication.util.CommonUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {


	private final JwtUtil jwtUtil;

	private final RefreshTokenService refreshTokenService;

	private final AuthenticationManager authenticationManager;

	private final UserService userService;

	private final UserSessionService userSessionService;



	public void validateEmail(ValidateEmailRequest request, String lang) {
		userService.validateEmail(request.getEmail(), lang);
	}

	public User signUp(SignUpRequest request, String lang) {
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

		return userService.createUser(createUserRequest, lang);
	}

	public WebTokenResponse signIn(SignInRequest request, String lang) {

        User user = userService.findByEmailWithException(request.getEmail(), lang);

		if (!isConsumer(user)) {
			throw new ApplicationException(HttpStatus.UNAUTHORIZED, ErrorCode.ERROR_UNAUTHORIZED_ACCESS, lang);
		}

		UsernamePasswordAuthenticationToken usernamePassAuthToken = new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword());

		Authentication authentication = authenticationManager.authenticate(usernamePassAuthToken);
		SecurityContextHolder.getContext().setAuthentication(authentication);

		String jwt = jwtUtil.generateAccessToken(authentication);
		if (jwt == null) {
			throw new ApplicationException(HttpStatus.FORBIDDEN, ErrorCode.ERROR_AUTHENTICATION_FAILED, lang);
		}

		UserSession userSession =  userSessionService.getActiveSessionByToken(jwt);
		if (userSession == null) {
			CreateUserSessionRequest createUserSessionRequest = CreateUserSessionRequest.builder()
                    .token(jwt)
                    .tokenType("Bearer")
                    .platformType(null)
                    .userDeviceId(null)
                    .userType("CUSTOMER")
                    .userId(user.getId())
                    .build();

			userSessionService.createNewSession(createUserSessionRequest);
		}

		String refreshToken = refreshTokenService.createRefreshToken(user.getId()).getToken();
		return new WebTokenResponse(jwt, refreshToken, "Bearer", user);
	}

	public WebTokenResponse signInAsAdmin(SignInRequest request, String lang) {
		User user = userService.findByEmailWithException(request.getEmail(), lang);

		UsernamePasswordAuthenticationToken usernamePassAuthToken = new UsernamePasswordAuthenticationToken(
				request.getEmail(), request.getPassword());

		if (!isAdmin(user)) {
			throw new ApplicationException(HttpStatus.UNAUTHORIZED, ErrorCode.ERROR_UNAUTHORIZED_ACCESS, lang);
		}

		Authentication authentication = authenticationManager.authenticate(usernamePassAuthToken);
		SecurityContextHolder.getContext().setAuthentication(authentication);

		String jwt = jwtUtil.generateAccessToken(authentication);
		if (jwt == null) {
			throw new ApplicationException(HttpStatus.FORBIDDEN, ErrorCode.ERROR_AUTHENTICATION_FAILED, lang);
		}

		UserSession userSession =  userSessionService.getActiveSessionByToken(jwt);
		if (userSession == null) {
			userSessionService.deactivatePreviousSession(user.getId());
			CreateUserSessionRequest createUserSessionRequest = CreateUserSessionRequest.builder().token(jwt).tokenType("Bearer").platformType(null).userDeviceId(null).userType("AUTHORITY").userId(user.getId()).build();
			userSessionService.createNewSession(createUserSessionRequest);
		}

		String refreshToken = refreshTokenService.createRefreshToken(user.getId()).getToken();
		return new WebTokenResponse(jwt, refreshToken, "Bearer", user);
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
		userService.getUserInfo(lang);

		String bearerToken = httpServletRequest.getHeader("Authorization");
		String accessToken = bearerToken.substring(7);
		log.info("Access Token: {}", accessToken);

		UserSession userSession = userSessionService.getActiveSessionByToken(accessToken);
		if (userSession != null) {
            userSessionService.deactivateSession(userSession);
		}
	}
}
