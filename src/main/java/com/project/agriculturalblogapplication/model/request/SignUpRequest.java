package com.project.agriculturalblogapplication.model.request;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/** An e-mail, a phone number or both; at least one (checked in {@code AuthService}). */
@Getter
@Setter
@ToString
public class SignUpRequest {

	@NotBlank(message = ErrorCode.ERROR_FIRST_NAME_IS_REQUIRED)
	private String name;

	@Schema(description = "Optional when a mobile number is given", example = "rahim@example.com")
	private String email;

	@Schema(description = "Country code; +880 when left out", example = "+880")
	private String countryCode;

	@Schema(description = "Optional when an e-mail is given", example = "1712345678")
	private String mobileNumber;

	@ToString.Exclude
	@NotBlank(message = ErrorCode.ERROR_PASSWORD_IS_REQUIRED)
	private String password;
}
