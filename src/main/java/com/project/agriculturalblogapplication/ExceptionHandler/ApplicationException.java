package com.project.agriculturalblogapplication.ExceptionHandler;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.http.HttpStatus;

@Getter
@Setter
@ToString
public class ApplicationException extends RuntimeException {

    private Object payload;

    private HttpStatus httpStatus;

    private String lang;

    public ApplicationException(String message) {
        super(message);
    }

    public ApplicationException(String message, String lang) {
        super(message);
        this.lang = lang;
    }

    public ApplicationException(HttpStatus httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public ApplicationException(HttpStatus httpStatus, String message, String languageCode) {
        super(message);
        this.httpStatus = httpStatus;
        this.lang = languageCode;
    }

    public ApplicationException(HttpStatus httpStatus, String message, Object payload, String languageCode) {
        super(message);
        this.payload = payload;
        this.httpStatus = httpStatus;
        this.lang = languageCode;
    }
}
