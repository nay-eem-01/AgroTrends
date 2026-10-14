package com.project.agriculturalblogapplication.model.request;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Why a moderator suspends an account; kept on the account and on its reports. */
@Getter
@Setter
public class BanRequest {

    @NotBlank(message = ErrorCode.ERROR_BAN_REASON_REQUIRED)
    @Size(max = 500, message = ErrorCode.ERROR_BAN_REASON_REQUIRED)
    private String reason;
}
