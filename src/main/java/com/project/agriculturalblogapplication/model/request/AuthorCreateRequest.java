package com.project.agriculturalblogapplication.model.request;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class AuthorCreateRequest {

    @NotNull(message = ErrorCode.ERROR_USER_ID_IS_REQUIRED)
    private Long userId;

    private ProfessionalInfoRequest professionalInfoRequest;
}
