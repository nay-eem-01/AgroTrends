package com.project.agriculturalblogapplication.model.request;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class VerifyPhoneRequest {

    @Schema(description = "Country code; +880 when left out", example = "+880")
    private String countryCode;

    @Schema(example = "1712345678")
    @NotBlank(message = ErrorCode.ERROR_MOBILE_NUMBER_IS_REQUIRED)
    private String mobileNumber;

    /** The 6-digit code from the SMS. */
    @ToString.Exclude
    @NotBlank(message = ErrorCode.ERROR_CODE_IS_REQUIRED)
    @Pattern(regexp = "\\d{6}", message = ErrorCode.ERROR_INVALID_OR_EXPIRED_CODE)
    private String code;
}
