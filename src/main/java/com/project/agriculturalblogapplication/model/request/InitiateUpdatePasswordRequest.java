package com.project.agriculturalblogapplication.model.request;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.enums.OTPChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class InitiateUpdatePasswordRequest {

	@NotBlank(message = ErrorCode.ERROR_CURRENT_PASSWORD_IS_REQUIRED)
	private String previousPassword;

	@NotBlank(message = ErrorCode.ERROR_NEW_PASSWORD_IS_REQUIRED)
	private String newPassword;

	@NotNull(message = ErrorCode.ERROR_OTP_CHANNEL_IS_REQUIRED)
	private OTPChannel otpChannel;
}
