package com.project.agriculturalblogapplication.ExceptionHandler;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class UnauthorizedActionException extends RuntimeException {
    public UnauthorizedActionException(String message) {
        super(message);
    }
}
