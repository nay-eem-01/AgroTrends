package com.project.agriculturalblogapplication.model.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class CreateAiResponseRequest {

    private String question;

    private String answer;

    private Long userId;
}
