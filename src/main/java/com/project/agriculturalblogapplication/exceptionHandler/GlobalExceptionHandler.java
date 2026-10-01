package com.project.agriculturalblogapplication.exceptionHandler;

import com.project.agriculturalblogapplication.model.response.HttpResponse;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Hidden
@Order(Ordered.LOWEST_PRECEDENCE)
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> resourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        HttpResponse errorResponse = new HttpResponse(HttpStatus.NOT_FOUND, false, ex.getMessage(), null);
        return new ResponseEntity<>(errorResponse, errorResponse.getStatus());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> globalExceptionHandler(Exception ex, WebRequest request) {
        // Never echo internals (SQL, Hibernate, stack details) to the client; give them an id to quote instead.
        String errorId = UUID.randomUUID().toString();
        log.error("Unhandled exception [errorId={}]", errorId, ex);
        HttpResponse errorResponse = new HttpResponse(HttpStatus.INTERNAL_SERVER_ERROR, false,
                "Something went wrong. Please try again later.", Map.of("errorId", errorId));
        return new ResponseEntity<>(errorResponse, errorResponse.getStatus());
    }
}
