package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.ExceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.Repositories.PrivilegeRepository;
import com.project.agriculturalblogapplication.Repositories.RoleRepository;
import com.project.agriculturalblogapplication.entities.Privilege;
import com.project.agriculturalblogapplication.entities.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PrivilegeService {

    private final PrivilegeRepository privilegeRepository;

	private final RoleRepository roleRepository;

	public List<Privilege> getAllPrivileges() {
		return privilegeRepository.findAll();
	}

	public Privilege findByPrivilegeId (Long privilegeId) {
		return privilegeRepository.findById(privilegeId).orElse(null);
	}

	public Privilege findByIdWithException(Long privilegeId) {
		return privilegeRepository.findById(privilegeId).orElseThrow(()->
				new ApplicationException(HttpStatus.NOT_FOUND, "Privilege not found id: " + privilegeId + "."));
	}
	
	public Privilege findByPrivilegeName (String name) {
		return privilegeRepository.findByPrivilegeNameEquals(name).orElse(null);
	}
	
	public Privilege createPrivilege(String name, String descName) {
		Privilege privilege = new Privilege();
		privilege.setPrivilegeName(name);
		privilege.setDescription(descName);
		return privilegeRepository.save(privilege);
	}

	public void deletePrivileges() {
		List<Role> roles = roleRepository.findAll();
		Privilege privilege = findByPrivilegeName("USER_UPDATE_FROM_ADMIN");
		if (privilege != null) {
			for (Role role : roles) {
				role.getPrivileges().remove(privilege);
				roleRepository.save(role);
			}
			privilegeRepository.delete(privilege);
		}
	}
}
