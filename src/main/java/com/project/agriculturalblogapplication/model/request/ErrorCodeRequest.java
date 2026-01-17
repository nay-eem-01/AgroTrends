package com.project.agriculturalblogapplication.model.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import static com.project.agriculturalblogapplication.constatnt.AppConstants.DEFAULT_LANGUAGE_CODE;

@Getter
@Setter
@ToString
public class ErrorCodeRequest {

    private Long id;

    private String internalCode;

    private String internalMessage;

    private String message;

    private String lang = DEFAULT_LANGUAGE_CODE;
}
