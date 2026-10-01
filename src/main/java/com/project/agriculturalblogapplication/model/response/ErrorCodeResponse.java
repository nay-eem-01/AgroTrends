package com.project.agriculturalblogapplication.model.response;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ErrorCodeResponse extends BaseResponse {

    private Long id;

    private String internalCode;

    private String internalMessage;

    private String message;
}
