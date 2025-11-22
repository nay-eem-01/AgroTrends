package com.project.agriculturalblogapplication.model.response;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class DefaultErrorResponse {
    private Integer status;
    private String timestamp;
    private String error;
    private String message;
    private String trace;
    private String path;
}
