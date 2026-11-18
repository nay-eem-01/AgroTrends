package com.project.agriculturalblogapplication.model.request;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/** A validated sign-up: e-mail or null, mobile number in E.164 or null, never both null. */
@Builder
@Getter
@ToString
public class CreateUserRequest {

    private String name;

    private String email;

    private String mobileNumber;

    @ToString.Exclude
    private String password;
}
