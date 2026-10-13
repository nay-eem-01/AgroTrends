package com.project.agriculturalblogapplication.model.request;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class SignUpRequest {

	@NotBlank(message = ErrorCode.ERROR_FIRST_NAME_IS_REQUIRED)
	private String name;

	@Email(message = ErrorCode.ERROR_INVALID_EMAIL)
	private String email;

	@Schema(description = "Country Code", example = "+880")
	@NotBlank(message = ErrorCode.ERROR_COUNTRY_CODE_IS_REQUIRED)
	private String countryCode;

	@Schema(description = "Mobile Number", example = "1717397409")
	@NotBlank(message = ErrorCode.ERROR_MOBILE_NUMBER_IS_REQUIRED)
	private String mobileNumber;

	@NotBlank(message = ErrorCode.ERROR_PASSWORD_IS_REQUIRED)
	private String password;
}
