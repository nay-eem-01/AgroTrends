package com.project.agriculturalblogapplication.exceptionHandler;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import com.project.agriculturalblogapplication.model.response.CustomResponse;
import com.project.agriculturalblogapplication.model.response.ErrorCodeResponse;
import com.project.agriculturalblogapplication.model.response.HttpResponse;
import com.project.agriculturalblogapplication.service.ErrorCodeService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
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
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.SocketTimeoutException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

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
		return buildResponseEntity((new HttpResponse(HttpStatus.valueOf(status.value()), false, error, null)));
	}

	@Override
	protected ResponseEntity<Object> handleConversionNotSupported(ConversionNotSupportedException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		// TODO Auto-generated method stub
		return buildResponseEntity((new HttpResponse(HttpStatus.valueOf(status.value()), false, "Invalid Input", null)));
	}

	@Override
	protected ResponseEntity<Object> handleHttpMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

		// TODO Auto-generated method stub
		return buildResponseEntity(
				(new HttpResponse(HttpStatus.valueOf(status.value()), false, "Invalid File Type provided.", null)));
	}

	@Override
	protected ResponseEntity<Object> handleHttpMessageNotWritable(HttpMessageNotWritableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

		// TODO Auto-generated method stub
		return buildResponseEntity(
				(new HttpResponse(HttpStatus.valueOf(status.value()), false, "Server Error. Write Failed", null)));
	}

	@Override
	protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

		// TODO Auto-generated method stub
		return buildResponseEntity((new HttpResponse(HttpStatus.valueOf(status.value()), false, "Invalid type of request.", null)));
	}

	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, @Nullable Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
		log.warn("Request failed with {}: {}", statusCode, ex.getMessage());

		String message = statusCode.is4xxClientError() ? ErrorCode.ERROR_INVALID_REQUEST : "Server Error Occurred.";
		return buildResponseEntity((new HttpResponse(HttpStatus.valueOf(statusCode.value()), false, message, null)));
	}

	// A query or path value that does not convert (season=WINTER, pageNo=abc): name the parameter and, for an
	// enum, list the accepted values. Never echo the rejected value or the conversion error.
	@Override
	protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		log.warn("Request parameter '{}' did not convert", ex.getPropertyName());
		return buildResponseEntity(new HttpResponse(HttpStatus.BAD_REQUEST, false, typeMismatchMessage(ex.getPropertyName(), ex.getRequiredType()), null));
	}

	@Override
	protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		return buildResponseEntity(new HttpResponse(HttpStatus.BAD_REQUEST, false,
				String.format(ErrorCode.ERROR_MISSING_PARAMETER, ex.getParameterName()), null));
	}

	static String typeMismatchMessage(String parameter, @Nullable Class<?> requiredType) {
		if (parameter == null) {
			return ErrorCode.ERROR_INVALID_REQUEST;
		}
		if (requiredType != null && requiredType.isEnum()) {
			String choices = Arrays.stream(requiredType.getEnumConstants())
					.map(constant -> ((Enum<?>) constant).name())
					.collect(Collectors.joining(", "));
			return String.format(ErrorCode.ERROR_INVALID_PARAMETER_CHOICE, parameter, choices);
		}
		return String.format(ErrorCode.ERROR_INVALID_PARAMETER_VALUE, parameter);
	}

	@Override
	protected ResponseEntity<Object> handleMissingPathVariable(MissingPathVariableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

		// TODO Auto-generated method stub
		return buildResponseEntity((new HttpResponse(HttpStatus.valueOf(status.value()), false, "Request Failed. Invalid Request. Please Try Again.", null)));
	}



	@Override
	protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

		// TODO Auto-generated method stub
		return buildResponseEntity((new HttpResponse(HttpStatus.valueOf(status.value()), false, "Request Failed. Invalid Request. Please Try Again.", null)));
	}

	@ResponseBody
	// Uploads over spring.servlet.multipart.max-file-size never reach ImageService; answer like it would.
	@Override
	protected ResponseEntity<Object> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex, HttpHeaders headers,
																		 HttpStatusCode status, WebRequest request) {
		return handleResponseException(new ApplicationException(HttpStatus.BAD_REQUEST, ErrorCode.ERROR_IMAGE_TOO_LARGE));
	}

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
		return buildResponseEntity(new HttpResponse(HttpStatus.REQUEST_TIMEOUT, false, "The server did not respond within the expected time. Please try again.", null));
	}

	@ResponseBody
	@ExceptionHandler(CustomResponseException.class)
	public CustomResponse handleCustomResponseException(CustomResponseException ex) {
		return new CustomResponse(ex.getStatus(), ex.getMessage(), ex.getPayload());
	}

	@ResponseBody
	@ExceptionHandler(SQLException.class)
	public ResponseEntity<Object> handleSQLException(SQLException ex) {
		log.error("SQL error", ex);
		return buildResponseEntity((new HttpResponse(HttpStatus.BAD_REQUEST, false, "Request Failed. Invalid Request. Please Try Again.", null)));
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
