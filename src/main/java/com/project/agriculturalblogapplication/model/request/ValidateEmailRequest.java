package com.project.agriculturalblogapplication.model.request;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ValidateEmailRequest {

    @Email(message = ErrorCode.ERROR_INVALID_EMAIL)
    @NotBlank(message = ErrorCode.ERROR_EMAIL_IS_REQUIRED)
    private String email;
}
