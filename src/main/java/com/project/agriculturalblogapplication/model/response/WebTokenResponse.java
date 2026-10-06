package com.project.agriculturalblogapplication.model.response;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class WebTokenResponse {

	private String accessToken;

	private String refreshToken;

	private String tokenType = "Bearer";

	private UserResponse user;
}
