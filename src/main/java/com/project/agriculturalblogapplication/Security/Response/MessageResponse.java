package com.project.agriculturalblogapplication.Security.Response;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MessageResponse {
    String message;

    public MessageResponse(String message) {
        this.message = message;
    }
}
