package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.repositories.RoleRepository;
import com.project.agriculturalblogapplication.repositories.UserRepository;
import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.entities.Privilege;
import com.project.agriculturalblogapplication.entities.Role;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.enums.RoleType;
import com.project.agriculturalblogapplication.model.request.CreateRoleRequest;
import com.project.agriculturalblogapplication.model.request.UpdateRoleRequest;
import com.project.agriculturalblogapplication.payloads.PaginationArgs;
import com.project.agriculturalblogapplication.util.CommonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class RoleService {

	private final RoleRepository roleRepository;

	private final PrivilegeService privilegeService;

	private final UserRepository userRepository;

	public List<Role> getAllRoles() {
		return roleRepository.findAll();
	}

	public Page<Role> getAllPaginatedRoles(PaginationArgs paginationArgs) {
		Pageable pageable = CommonUtils.getPageable(paginationArgs);

		return roleRepository.findAll(pageable);
	}

	public List<Role> getAllUserRoles(String username) {
		User user = userRepository.findTopByEmailEqualsIgnoreCase(username).orElseThrow(()->
				new ApplicationException(HttpStatus.NOT_FOUND, "User not found with username: " + username + "."));

		return new ArrayList<>(user.getRoles());
	}

	public List<String> getAllRoleTypes() {
		return Arrays.stream(RoleType.values()).map(Enum::name).toList();
	}

	public List<Role> findAllByRoleType(RoleType roleType) {
		return roleRepository.findAllByRoleType(roleType);
	}


	public Role findById(Long roleId) {
		return roleRepository.findById(roleId).orElse(null);
	}

	public Role findByIdWithException(Long roleId, String lang) {
		return roleRepository.findById(roleId).orElseThrow(() ->
				new ApplicationException(HttpStatus.NOT_FOUND, ErrorCode.ERROR_ROLE_NOT_FOUND, lang));
	}
	
	public Role findRoleByName(String roleName){
		return roleRepository.findTopByRoleName(roleName).orElse(null);
	}

	public Role findRoleByNameWithException(String roleName, String lang) {
		return roleRepository.findTopByRoleName(roleName).orElseThrow(() ->
				new ApplicationException(HttpStatus.NOT_FOUND, ErrorCode.ERROR_ROLE_NOT_FOUND, lang)
		);
	}

	public Boolean existsByRoleName(String roleName) {
		return roleRepository.existsByRoleName(roleName);
	}

	public Boolean existsByRoleNameAndIdNot(String roleName, Long roleId) {
		return roleRepository.existsByRoleNameAndIdNot(roleName, roleId);
	}

	public Role saveRole (Role role) {
		return roleRepository.save(role);
	}

	public void createRole(String roleName, RoleType roleType, String description, List<Privilege> privileges) {
		Role role = new Role();
		role.setRoleName(roleName);
		role.setRoleType(roleType);
		role.setPrivileges(privileges);
		saveRole(role);
	}

	public Role createRole(CreateRoleRequest request, String lang) {
		requireAdminType(request.getRoleType(), lang);
		if (existsByRoleName(request.getRoleName())) {
			throw new ApplicationException(HttpStatus.BAD_REQUEST, ErrorCode.ERROR_ROLE_ALREADY_EXISTS, lang);
		}

		Role role = new Role();
		role.setRoleName(request.getRoleName());
		role.setRoleType(request.getRoleType());
		role.setPrivileges(grantablePrivileges(request.getPrivilegeIds(), lang));
		return saveRole(role);
	}

	public Role updateRole(UpdateRoleRequest request, String lang) {
		Role role = findByIdWithException(request.getId(), lang);
		requireAdminType(role.getRoleType(), lang);
		requireAdminType(request.getRoleType(), lang);

		if (existsByRoleNameAndIdNot(request.getRoleName(), role.getId())) {
			throw new ApplicationException(HttpStatus.BAD_REQUEST, ErrorCode.ERROR_ROLE_ALREADY_EXISTS, lang);
		}

		role.setRoleName(request.getRoleName());
		role.setPrivileges(grantablePrivileges(request.getPrivilegeIds(), lang));
		return saveRole(role);
	}

	public void deleteRole(Long roleId, String lang) {
		Role role = findByIdWithException(roleId, lang);
		requireAdminType(role.getRoleType(), lang);
		if(userRepository.existsByRolesIn(Set.of(role))) {
			throw new ApplicationException(HttpStatus.BAD_REQUEST, ErrorCode.ERROR_USER_ALREADY_ASSIGNED_WITH_ROLE, lang);
		}

		roleRepository.delete(role);
	}

	/** Gives the user an ADMIN-type role; their privileges change on their next request. */
	@Transactional
	public User grantRole(Long userId, Long roleId, String lang) {
		Role role = findByIdWithException(roleId, lang);
		requireAdminType(role.getRoleType(), lang);
		User user = userWithException(userId, lang);
		user.getRoles().add(role);
		return userRepository.save(user);
	}

	@Transactional
	public User removeRole(Long userId, Long roleId, String lang) {
		Role role = findByIdWithException(roleId, lang);
		requireAdminType(role.getRoleType(), lang);
		User user = userWithException(userId, lang);
		user.getRoles().removeIf(r -> r.getId().equals(roleId));
		return userRepository.save(user);
	}

	/** Only ADMIN roles are managed through the API: the USER and SUPER_ADMIN roles are seeded and fixed. */
	private static void requireAdminType(RoleType type, String lang) {
		if (type != RoleType.ADMIN) {
			throw new ApplicationException(HttpStatus.BAD_REQUEST, ErrorCode.ERROR_ONLY_ADMIN_ROLES, lang);
		}
	}

	/** ROLE_* privileges stay with the super-admin, so an admin role can never hand out more admin roles. */
	private List<Privilege> grantablePrivileges(List<Long> privilegeIds, String lang) {
		if (privilegeIds == null || privilegeIds.isEmpty()) {
			throw new ApplicationException(HttpStatus.BAD_REQUEST, ErrorCode.ERROR_PLEASE_SELECT_PRIVILEGES, lang);
		}
		List<Privilege> privileges = new ArrayList<>();
		for (Long privilegeId : new LinkedHashSet<>(privilegeIds)) {
			Privilege privilege = privilegeService.findByIdWithException(privilegeId);
			if (!isGrantable(privilege)) {
				throw new ApplicationException(HttpStatus.BAD_REQUEST, ErrorCode.ERROR_PRIVILEGE_NOT_GRANTABLE, lang);
			}
			privileges.add(privilege);
		}
		return privileges;
	}

	public static boolean isGrantable(Privilege privilege) {
		return !privilege.getPrivilegeName().startsWith("ROLE_");
	}

	private User userWithException(Long userId, String lang) {
		return userRepository.findById(userId).orElseThrow(() ->
				new ApplicationException(HttpStatus.NOT_FOUND, ErrorCode.ERROR_USER_NOT_FOUND, lang));
	}
}
