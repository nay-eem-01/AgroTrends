package com.project.agriculturalblogapplication.security.model.request;
import com.project.agriculturalblogapplication.security.enums.PlatformType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@Getter
@Setter
@ToString
public class CreateUserSessionRequest {

    private String token;

    private String tokenType;

    private PlatformType platformType;

    private Long userDeviceId;

    private String userType;

    private Long userId;
}
