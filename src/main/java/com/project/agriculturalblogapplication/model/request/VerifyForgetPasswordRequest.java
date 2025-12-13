package com.project.agriculturalblogapplication.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class VerifyForgetPasswordRequest {

    @NotBlank(message = "ERROR_EMAIL_IS_REQUIRED")
    private String email;

    @NotBlank(message = "ERROR_OTP_TOKEN_IS_REQUIRED")
    private String otpToken;

    @NotBlank(message = "ERROR_OTP_CODE_IS_REQUIRED")
    private String otpCode;
}
