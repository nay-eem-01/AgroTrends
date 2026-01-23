package com.project.agriculturalblogapplication.model.request;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.entities.User;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class AuthorCreateRequest {

    @NotNull(message = ErrorCode.ERROR_USER_IS_REQUIRED)
    private User user;

    private ProfessionalInfoRequest professionalInfoRequest;
}
