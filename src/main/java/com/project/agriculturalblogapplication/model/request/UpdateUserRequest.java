package com.project.agriculturalblogapplication.model.request;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class UpdateUserRequest {

    @NotBlank(message = ErrorCode.ERROR_NAME_IS_REQUIRED)
    private String name;

    @NotBlank(message = ErrorCode.ERROR_EMAIL_IS_REQUIRED)
    private String email;

    @NotBlank(message = ErrorCode.ERROR_COUNTRY_CODE_IS_REQUIRED)
    private String countryCode;

    @NotBlank(message = ErrorCode.ERROR_MOBILE_NUMBER_IS_REQUIRED)
    private String mobileNumber;
}
