package com.project.agriculturalblogapplication.exceptionHandler;

import com.project.agriculturalblogapplication.model.response.HttpResponse;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;



@Hidden
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@ControllerAdvice
public class ExceptionAuthHandlingController {
	@ResponseBody
	@ExceptionHandler(value = AuthenticationException.class)
	public ResponseEntity<Object> handleAuthenticationExceptions(AuthenticationException ex,
			HttpServletResponse response) {
		return new ResponseEntity<>(new HttpResponse(HttpStatus.UNAUTHORIZED, false, "Invalid email or password.", null), HttpStatus.UNAUTHORIZED);
	}

	@ResponseBody
	@ExceptionHandler(value = AccessDeniedException.class)
	public ResponseEntity<Object> handleAccessDenied(AccessDeniedException ex) {
		return new ResponseEntity<>(new HttpResponse(HttpStatus.FORBIDDEN, false, "You do not have permission to perform this action.", null), HttpStatus.FORBIDDEN);
	}

	@ExceptionHandler(value = NoHandlerFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public ResponseEntity<Object> handleNoHandlerFound(NoHandlerFoundException ex, WebRequest request) {
		return new ResponseEntity<>(new HttpResponse(HttpStatus.NOT_FOUND, false, "Resource not found.", null), HttpStatus.NOT_FOUND);
	}

	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	@ExceptionHandler(value = BadCredentialsException.class)
	public ResponseEntity<Object> handleBadCredentialException(BadCredentialsException ex) {
		return new ResponseEntity<>(new HttpResponse(HttpStatus.UNAUTHORIZED, false, "Invalid email or password.", null), HttpStatus.UNAUTHORIZED);
	}
}
