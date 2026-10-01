package com.project.agriculturalblogapplication.config;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.security.SecureRandom;
import java.util.*;

import static com.project.agriculturalblogapplication.constatnt.AppConstants.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class InitialDataLoader implements ApplicationListener<ApplicationReadyEvent> {

    /** Credentials shipped by earlier versions of this project; any account still using them is locked down. */
    private static final String LEGACY_ADMIN_EMAIL = "admin@gmail.com";
    private static final String LEGACY_DEFAULT_PASSWORD = "123456";

    private boolean alreadySetup = false;
    private final UserService userService;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;
    private final PrivilegeService privilegeService;

    @Value("${app.admin.initial-email}")
    private String initialAdminEmail;

    @Value("${app.admin.initial-password:}")
    private String initialAdminPassword;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
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

        lockDownLegacyDefaultAdmin();

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

        String password = initialAdminPassword;
        boolean generated = password == null || password.isBlank();
        if (generated) {
            password = generatePassword();
        }

        User superAdminUser = new User();
        superAdminUser.setName(INITIAL_ROLE);
        superAdminUser.setEmail(initialAdminEmail);
        superAdminUser.setMobileNumber(INITIAL_MOBILE_NUMBER);
        superAdminUser.setPassword(passwordEncoder.encode(password));
        superAdminUser.setMustChangePassword(true);
        superAdminUser.setRoles(roles);
        superAdminUser.setUserTypes(Set.of(UserType.AUTHOR.name()));
        userService.saveUser(superAdminUser);

        if (generated) {
            log.warn("Super-admin account '{}' created with a generated one-time password: {} " +
                    "(shown only once; it must be changed on first sign-in)", initialAdminEmail, password);
        } else {
            log.warn("Super-admin account '{}' created from INITIAL_ADMIN_PASSWORD; it must be changed on first sign-in.",
                    initialAdminEmail);
        }

        alreadySetup = true;
    }

    /**
     * Databases seeded by earlier versions contain admin@gmail.com / 123456. If that account (or the configured
     * admin) still has the well-known password, replace it with a random one and force a password change.
     */
    private void lockDownLegacyDefaultAdmin() {
        for (String email : new LinkedHashSet<>(List.of(LEGACY_ADMIN_EMAIL, initialAdminEmail))) {
            User admin = userService.findByEmail(email);
            if (admin == null || !passwordEncoder.matches(LEGACY_DEFAULT_PASSWORD, admin.getPassword())) {
                continue;
            }
            String password = generatePassword();
            admin.setPassword(passwordEncoder.encode(password));
            admin.setMustChangePassword(true);
            userService.saveUser(admin);
            log.warn("Account '{}' still used the default password and was reset. New one-time password: {} " +
                    "(shown only once; it must be changed on next sign-in)", email, password);
        }
    }

    private static String generatePassword() {
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder("Aa1!");
        for (int i = 0; i < 16; i++) {
            sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return sb.toString();
    }

    private boolean checkIfPrivilegeExist (String privilegeName) {
        return privilegeService.findByPrivilegeName(privilegeName) != null;
    }

    private boolean checkIfSuperAdminExist () {
        return userService.findByEmail(initialAdminEmail) != null;
    }

    private boolean checkIfRoleExist (String roleName) {
        return roleService.findRoleByName(roleName) != null;
    }
}
