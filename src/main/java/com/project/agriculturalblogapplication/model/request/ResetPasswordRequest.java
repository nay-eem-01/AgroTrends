package com.project.agriculturalblogapplication.model.request;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ResetPasswordRequest {

    @NotBlank(message = ErrorCode.ERROR_EMAIL_IS_REQUIRED)
    private String email;

    @NotBlank(message = ErrorCode.ERROR_OTP_TOKEN_IS_REQUIRED)
    private String otpToken;

    @NotBlank(message = ErrorCode.ERROR_NEW_PASSWORD_IS_REQUIRED)
    private String newPassword;
}
