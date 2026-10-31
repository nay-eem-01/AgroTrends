package com.project.agriculturalblogapplication.model.request;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** The caller's own public profile facts; identity comes from the token. Headline and bio are on the author profile. */
@Getter
@Setter
public class UpdateUserProfileRequest {
    @Size(max = 80, message = ErrorCode.ERROR_LOCATION_TOO_LONG)
    private String location;
    /** From {@code POST /api/images}. */
    private String coverUrl;
}
