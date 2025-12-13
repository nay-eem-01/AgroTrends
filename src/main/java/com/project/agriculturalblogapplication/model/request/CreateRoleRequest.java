package com.project.agriculturalblogapplication.model.request;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.enums.RoleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CreateRoleRequest {

	@NotBlank(message = ErrorCode.ERROR_ROLE_NAME_IS_REQUIRED)
	private String roleName;

	@NotNull(message = ErrorCode.ERROR_ROLE_TYPE_IS_REQUIRED)
	private RoleType roleType;

	private String description;

	private List<Long> privilegeIds = new ArrayList<>();
}
