package com.project.agriculturalblogapplication.model.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CustomResponse {

    private String status;

    private String message;

    private Object payload;
}
