package com.project.agriculturalblogapplication.model.request;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** The caller's own author profile; identity comes from the token. */
@Getter
@Setter
public class UpdateAuthorProfileRequest {
    @NotBlank(message = ErrorCode.ERROR_DESIGNATION_IS_REQUIRED)
    private String designation;
    @NotEmpty(message = ErrorCode.ERROR_SPECIALITY_IS_REQUIRED)
    private List<String> specialities;
    private String occupation;
    private String workPlaceOrInstitution;
    @Size(max = 2000, message = ErrorCode.ERROR_BIO_TOO_LONG)
    private String bio;
    private String profileImageUrl;
}
