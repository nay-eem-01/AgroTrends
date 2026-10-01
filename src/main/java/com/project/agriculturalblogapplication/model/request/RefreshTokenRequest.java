package com.project.agriculturalblogapplication.model.request;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefreshTokenRequest {

    @NotBlank(message = ErrorCode.ERROR_TOKEN_IS_REQUIRED)
    private String refreshToken;
}
