package com.project.agriculturalblogapplication.exceptionHandler;

import com.project.agriculturalblogapplication.model.response.HttpResponse;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.lang.Nullable;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.SocketTimeoutException;
import java.sql.SQLException;
import java.util.Optional;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
@RequiredArgsConstructor
@Hidden
public class ExceptionHandlingController extends ResponseEntityExceptionHandler {

	private final ErrorCodeService errorCodeService;

	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		String error = "Invalid input.";
		return buildResponseEntity((new HttpResponse(HttpStatus.valueOf(status.value()), false, error, ex.getLocalizedMessage())));
	}

	@Override
	protected ResponseEntity<Object> handleConversionNotSupported(ConversionNotSupportedException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		// TODO Auto-generated method stub
		return buildResponseEntity((new HttpResponse(HttpStatus.valueOf(status.value()), false, "Invalid Input", ex.getLocalizedMessage())));
	}

	@Override
	protected ResponseEntity<Object> handleHttpMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

		// TODO Auto-generated method stub
		return buildResponseEntity(
				(new HttpResponse(HttpStatus.valueOf(status.value()), false, "Invalid File Type provided.", ex.getLocalizedMessage())));
	}

	@Override
	protected ResponseEntity<Object> handleHttpMessageNotWritable(HttpMessageNotWritableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

		// TODO Auto-generated method stub
		return buildResponseEntity(
				(new HttpResponse(HttpStatus.valueOf(status.value()), false, "Server Error. Write Failed", ex.getLocalizedMessage())));
	}

	@Override
	protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

		// TODO Auto-generated method stub
		return buildResponseEntity((new HttpResponse(HttpStatus.valueOf(status.value()), false, "Invalid type of request.", ex.getLocalizedMessage())));
	}

	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, @Nullable Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {

		// TODO Auto-generated method stub
		return buildResponseEntity((new HttpResponse(HttpStatus.valueOf(statusCode.value()), false, "Server Error Occurred.", ex.getLocalizedMessage())));
	}

	@Override
	protected ResponseEntity<Object> handleMissingPathVariable(MissingPathVariableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

		// TODO Auto-generated method stub
		return buildResponseEntity((new HttpResponse(HttpStatus.valueOf(status.value()), false, "Request Failed. Invalid Request. Please Try Again.",
				ex.getLocalizedMessage())));
	}



	@Override
	protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

		// TODO Auto-generated method stub
		return buildResponseEntity((new HttpResponse(HttpStatus.valueOf(status.value()), false, "Request Failed. Invalid Request. Please Try Again.",
				ex.getLocalizedMessage())));
	}

	@ResponseBody
	@ExceptionHandler(ApplicationException.class)
	public ResponseEntity<Object> handleResponseException(ApplicationException ex) {
		HttpStatus httpStatus = ex.getHttpStatus();
		String errorCode = ex.getMessage();
		String lang = ex.getLang();
		Object payload = ex.getPayload();

		String message = errorCode;
		ErrorCodeResponse errorCodeResponse = errorCodeService.findByInternalCode(errorCode, lang);
		if (errorCodeResponse != null) {
			message = errorCodeResponse.getMessage();
			if (message == null) {
				message = errorCodeResponse.getInternalMessage();
			}
		}

		log.error("ResponseException: {} : {}", httpStatus, message);
		HttpResponse errorResponse = new HttpResponse(httpStatus != null ? httpStatus :  HttpStatus.BAD_REQUEST, false, message, payload);
		return buildResponseEntity(errorResponse);
	}

	@ResponseBody
	@ExceptionHandler(SocketTimeoutException.class)
	public ResponseEntity<Object> handleSocketTimeoutException(SocketTimeoutException ex) {
		return buildResponseEntity(new HttpResponse(HttpStatus.REQUEST_TIMEOUT, false, "The server did not respond within the expected time. Please try again.", ex.getLocalizedMessage()));
	}

	@ResponseBody
	@ExceptionHandler(CustomResponseException.class)
	public CustomResponse handleCustomResponseException(CustomResponseException ex) {
		return new CustomResponse(ex.getStatus(), ex.getMessage(), ex.getPayload());
	}

	@ResponseBody
	@ExceptionHandler(SQLException.class)
	public ResponseEntity<Object> handleSQLException(SQLException ex) {
		return buildResponseEntity((new HttpResponse(HttpStatus.BAD_REQUEST, false, "Request Failed. Invalid Request. Please Try Again.",
				ex.getLocalizedMessage())));
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		HttpResponse errorResponse = new HttpResponse(HttpStatus.valueOf(status.value()), false, "Invalid request body.", null);
		Optional<ObjectError> objectError = ex.getBindingResult().getAllErrors().stream().findFirst();

		if(objectError.isPresent()) {
			ObjectError error = objectError.get();
			errorResponse.setMessage(error.getDefaultMessage());

			return buildResponseEntity(errorResponse);
		}

		return buildResponseEntity(errorResponse);
	}

	private ResponseEntity<Object> buildResponseEntity(HttpResponse apiError) {
		return new ResponseEntity<>(apiError, apiError.getStatus());
	}
}
