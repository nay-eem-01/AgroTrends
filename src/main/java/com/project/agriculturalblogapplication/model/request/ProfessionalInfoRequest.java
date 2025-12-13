package com.project.agriculturalblogapplication.model.request;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class ProfessionalInfoRequest {

    @NotBlank(message = ErrorCode.ERROR_DESIGNATION_IS_REQUIRED)
    private String designation;

    @NotEmpty(message = ErrorCode.ERROR_SPECIALITY_IS_REQUIRED)
    private List<String> specialities;

    private String profileImageUrl;

    private String institution;

    private String professionalStatement;
}
