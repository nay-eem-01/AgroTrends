package com.project.agriculturalblogapplication.model.request;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyEmailRequest {

    @NotBlank(message = ErrorCode.ERROR_TOKEN_IS_REQUIRED)
    @Size(max = 128)
    private String token;

    /** Hand-written so the token never reaches a log line. */
    @Override
    public String toString() {
        return "VerifyEmailRequest[token=***]";
    }
}
