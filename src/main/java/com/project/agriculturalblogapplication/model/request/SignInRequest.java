package com.project.agriculturalblogapplication.model.request;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class SignInRequest {

    @NotBlank(message = ErrorCode.ERROR_EMAIL_IS_REQUIRED)
    private String email;

    @NotBlank(message = ErrorCode.ERROR_PASSWORD_IS_REQUIRED)
	private String password;
}
