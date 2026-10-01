package com.project.agriculturalblogapplication.model.response;


import com.project.agriculturalblogapplication.entities.User;
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

	private User user;
}
