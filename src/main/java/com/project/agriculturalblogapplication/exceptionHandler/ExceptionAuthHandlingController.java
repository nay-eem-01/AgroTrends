package com.project.agriculturalblogapplication.exceptionHandler;

import com.project.agriculturalblogapplication.model.response.HttpResponse;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;



@Hidden
@ControllerAdvice
public class ExceptionAuthHandlingController {
	@ResponseBody
	@ExceptionHandler(value = AuthenticationException.class)
	public ResponseEntity<Object> handleAuthenticationExceptions(AuthenticationException ex,
			HttpServletResponse response) {
		return new ResponseEntity<>(new HttpResponse(HttpStatus.FORBIDDEN, false, "Invalid email/password provided.", null), HttpStatus.FORBIDDEN);
	}

	@ExceptionHandler(value = NoHandlerFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public ResponseEntity<Object> handleNoHandlerFound(NoHandlerFoundException ex, WebRequest request) {
		return new ResponseEntity<>(new HttpResponse(HttpStatus.NOT_FOUND, false, "Application cannot reach to server", ex.getLocalizedMessage()), HttpStatus.NOT_FOUND);
	}

	@ResponseStatus(HttpStatus.FORBIDDEN)
	@ExceptionHandler(value = BadCredentialsException.class)
	public ResponseEntity<Object> handleBadCredentialException(BadCredentialsException ex) {
		return new ResponseEntity<>(new HttpResponse(HttpStatus.FORBIDDEN, false, "Username or password is incorrect. Please try with correct credentials.", null), HttpStatus.FORBIDDEN);
	}
}
