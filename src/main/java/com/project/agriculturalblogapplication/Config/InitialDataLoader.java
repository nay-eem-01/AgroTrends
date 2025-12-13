package com.project.agriculturalblogapplication.Config;

import com.project.agriculturalblogapplication.constatnt.AppConstants;
import com.project.agriculturalblogapplication.entities.Privilege;
import com.project.agriculturalblogapplication.entities.Role;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.enums.RoleType;
import com.project.agriculturalblogapplication.enums.UserType;
import com.project.agriculturalblogapplication.service.PrivilegeService;
import com.project.agriculturalblogapplication.service.RoleService;
import com.project.agriculturalblogapplication.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationContextEvent;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.util.*;

import static com.project.agriculturalblogapplication.constatnt.AppConstants.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class InitialDataLoader implements ApplicationListener<ApplicationContextEvent>{

    private boolean alreadySetup = false;
    private final UserService userService;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;
    private final PrivilegeService privilegeService;

    @Override
    public void onApplicationEvent(ApplicationContextEvent event) {
        List<Privilege> superAdminPrivileges = new ArrayList<>();

        for (Map.Entry<String, String> entry : AppConstants.PERMISSIONS.entrySet()) {
            boolean ifPrivilegeExists = this.checkIfPrivilegeExist(entry.getKey());

            if (!ifPrivilegeExists) {
                Privilege newPrivilege = privilegeService.createPrivilege(entry.getKey(),entry.getValue());
                superAdminPrivileges.add(newPrivilege);
            }
        }

        if(checkIfRoleExist(AppConstants.INITIAL_ROLE)) {
            Role superAdminRole = roleService.findRoleByName(AppConstants.INITIAL_ROLE);
            superAdminRole.getPrivileges().addAll(superAdminPrivileges);
            roleService.saveRole(superAdminRole);
        }

        if (alreadySetup || checkIfSuperAdminExist()) {
            return;
        }

        List<Privilege> consumerPrivileges = new ArrayList<>();
        Privilege newPrivilege = privilegeService.createPrivilege(CONSUMER_PERMISSIONS, CONSUMER_PERMISSIONS_DESC);

        superAdminPrivileges.add(newPrivilege);
        consumerPrivileges.add(newPrivilege);

        roleService.createRole(USER_ROLE, RoleType.USER, null, consumerPrivileges);
        roleService.createRole(AppConstants.INITIAL_ROLE, RoleType.SUPER_ADMIN, null, superAdminPrivileges);

        Set<Role> roles = new HashSet<>();
        Role role = roleService.findRoleByName(AppConstants.INITIAL_ROLE);
        if (role != null) {
            roles.add(role);
        }

        User superAdminUser = new User();
        superAdminUser.setName(INITIAL_ROLE);
        superAdminUser.setEmail(INITIAL_USERNAME);
        superAdminUser.setMobileNumber(INITIAL_MOBILE_NUMBER);
        superAdminUser.setPassword(passwordEncoder.encode(AppConstants.INITIAL_PASSWORD));
        superAdminUser.setRoles(roles);
        superAdminUser.setUserTypes(Set.of(UserType.AUTHOR.name()));
       userService.saveUser(superAdminUser);

        alreadySetup = true;
    }

    private boolean checkIfPrivilegeExist (String privilegeName) {
        return privilegeService.findByPrivilegeName(privilegeName) != null;
    }

    private boolean checkIfSuperAdminExist () {
        return userService.findByEmail(INITIAL_USERNAME) != null;
    }

    private boolean checkIfRoleExist (String roleName) {
        return roleService.findRoleByName(roleName) != null;
    }
}