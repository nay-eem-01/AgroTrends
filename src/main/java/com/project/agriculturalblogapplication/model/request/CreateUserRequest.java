package com.project.agriculturalblogapplication.model.request;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@Getter
@Setter
@ToString
public class CreateUserRequest {

    private String name;

    private String email;

    private String countryCode;

    private String mobileNumber;

    private String password;
}
