package com.project.agriculturalblogapplication.exceptionHandler;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomResponseException extends RuntimeException {

    private String status;

    private Object payload;

    public CustomResponseException(String message) {
        super(message);
    }

    public CustomResponseException(String status, String message) {
        super(message);
        this.status = status;
    }

    private CustomResponseException(String status, String message, Object payload) {
        super(message);
        this.status = status;
        this.payload = payload;
    }
}
