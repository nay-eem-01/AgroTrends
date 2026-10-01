package com.project.agriculturalblogapplication.model.request;

import com.project.agriculturalblogapplication.enums.UserType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.util.Set;

@Builder
@Getter
@Setter
@ToString
public class CreateUserRequest {

    private Set<UserType> userTypes;

    private String name;

    private String email;

    private String countryCode;

    private String mobileNumber;

    private String password;

    private ProfessionalInfoRequest professionalInfoRequest;
}
