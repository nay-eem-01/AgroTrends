package com.project.agriculturalblogapplication.model.response;

import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class VerifyOTPResponse {

    private Boolean isVerified;

    private String otpToken;
}
