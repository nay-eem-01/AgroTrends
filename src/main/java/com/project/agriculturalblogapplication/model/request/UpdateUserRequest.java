package com.project.agriculturalblogapplication.model.request;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * The caller's own profile. E-mail and mobile number are each optional, but the account keeps at least one;
 * leaving one out removes it, a new or changed one has to be confirmed again.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class UpdateUserRequest {

    @NotBlank(message = ErrorCode.ERROR_NAME_IS_REQUIRED)
    private String name;

    @Schema(example = "rahim@example.com")
    private String email;

    @Schema(description = "Country code; +880 when left out", example = "+880")
    private String countryCode;

    @Schema(example = "1712345678")
    private String mobileNumber;
}
