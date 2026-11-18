package com.project.agriculturalblogapplication.model.request;

import com.fasterxml.jackson.annotation.JsonAlias;
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

    /** An e-mail address or a phone number (+8801712345678, or 01712345678 for Bangladesh). */
    @Schema(example = "01712345678")
    @JsonAlias("email")
    @NotBlank(message = ErrorCode.ERROR_EMAIL_OR_PHONE_IS_REQUIRED)
    private String identifier;

    @ToString.Exclude
    @NotBlank(message = ErrorCode.ERROR_PASSWORD_IS_REQUIRED)
	private String password;
}
