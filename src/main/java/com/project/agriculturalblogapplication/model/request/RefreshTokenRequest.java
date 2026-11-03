package com.project.agriculturalblogapplication.model.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefreshTokenRequest {

    /** Optional: without it the refresh endpoint reads the agrotrends_refresh cookie. */
    private String refreshToken;
}
