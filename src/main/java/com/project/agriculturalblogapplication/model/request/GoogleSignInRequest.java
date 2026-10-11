package com.project.agriculturalblogapplication.model.request;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.enums.UserType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class GoogleSignInRequest {

    @NotBlank(message = ErrorCode.ERROR_TOKEN_IS_REQUIRED)
    @Size(max = 4096)
    @Schema(description = "The credential from Google Identity Services (a Google-signed ID token)")
    private String idToken;

    @Schema(description = "Used only when this creates the account, as at sign-up. Omitted means a reader (CONSUMER).")
    private Set<UserType> userType;

    /** Hand-written so the token never reaches a log line. */
    @Override
    public String toString() {
        return "GoogleSignInRequest[idToken=***, userType=" + userType + "]";
    }
}
