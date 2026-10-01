package com.project.agriculturalblogapplication.model.response;

import com.project.agriculturalblogapplication.enums.OTPChannel;
import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class SendVerificationCodeResponse {

    private String otpToken;

    private OTPChannel otpChannel;

    private Boolean authenticatorAppEnable;

    private String email;

    private String mobileNumber;
}
