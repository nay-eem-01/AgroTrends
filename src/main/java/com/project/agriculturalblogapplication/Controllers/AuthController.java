package com.project.agriculturalblogapplication.Controllers;

import com.project.agriculturalblogapplication.Models.APP_ROLE;
import com.project.agriculturalblogapplication.Models.Roles;
import com.project.agriculturalblogapplication.Models.Users;
import com.project.agriculturalblogapplication.Repositories.RoleRepository;
import com.project.agriculturalblogapplication.Repositories.UserRepositories;
import com.project.agriculturalblogapplication.Security.Request.LogInRequest;
import com.project.agriculturalblogapplication.Security.Request.SignUpRequest;
import com.project.agriculturalblogapplication.Security.Response.MessageResponse;
import com.project.agriculturalblogapplication.Security.Response.UserResponse;
import com.project.agriculturalblogapplication.Security.UserDetailsImpl;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("app/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepositories userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AuthController(AuthenticationManager authenticationManager,
                          UserRepositories userRepository,
                          RoleRepository roleRepository,
                          PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/signin")
    public ResponseEntity<?> userSignIn(@RequestBody LogInRequest logInRequest, HttpServletRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(logInRequest.getUserName(), logInRequest.getPassword()));
        } catch (AuthenticationException e) {
            Map<String, Object> map = new HashMap<>();
            map.put("message", "Bad credentials");
            map.put("status", false);
            return new ResponseEntity<>(map, HttpStatus.UNAUTHORIZED);
        }

        // Set authentication in SecurityContext
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Create session and store SecurityContext
        HttpSession session = request.getSession(true);
        session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

        // Prepare user response
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return ResponseEntity.ok(new UserResponse(userDetails.getId(), userDetails.getUsername(), roles));
    }

    @Transactional
    @PostMapping("/signup")
    public ResponseEntity<?> userSignUp(@Valid @RequestBody SignUpRequest signUpRequest) {
        if (userRepository.existsByUserName(signUpRequest.getUserName())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Username is already taken"));
        }
        if (userRepository.existsByUserEmail(signUpRequest.getEmail())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Email is already in use"));
        }

        Users user = new Users(
                signUpRequest.getUserName(),
                signUpRequest.getEmail(),
                passwordEncoder.encode(signUpRequest.getPassword())
        );

        Set<Roles> roles = resolveRoles(signUpRequest.getRole());
        user.setRoles(roles);
        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }

    private Set<Roles> resolveRoles(Set<String> strRoles) {
        Set<Roles> roles = new HashSet<>();
        if (strRoles == null || strRoles.isEmpty()) {
            roles.add(getRole(APP_ROLE.ROLE_USER));
        } else {
            strRoles.forEach(role -> {
                switch (role.toLowerCase()) {
                    case "admin":
                        roles.add(getRole(APP_ROLE.ROLE_ADMIN));
                        break;
                    default:
                        roles.add(getRole(APP_ROLE.ROLE_USER));
                }
            });
        }
        return roles;
    }

    private Roles getRole(APP_ROLE roleEnum) {
        return roleRepository.findByRole(roleEnum)
                .orElseThrow(() -> new RuntimeException("Error: Role not found"));
    }

    @GetMapping("/username")
    public String getCurrentUserName(Authentication authentication) {
        return (authentication != null) ? authentication.getName() : "";
    }

    @GetMapping("/userdetails")
    public ResponseEntity<?> getCurrentUserDetails(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authenticated");
        }

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return ResponseEntity.ok(new UserResponse(userDetails.getId(), userDetails.getUsername(), roles));
    }

    @PostMapping("/signout")
    public ResponseEntity<?> signOut(HttpServletRequest request, HttpServletResponse response) {
        // Clear authentication from SecurityContext
        SecurityContextHolder.clearContext();

        // Invalidate session if exists
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        // Clear JSESSIONID cookie
        Cookie cookie = new Cookie("JSESSIONID", null);
        cookie.setPath(request.getContextPath());
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        return ResponseEntity.ok(new MessageResponse("You have been signed out"));
    }
}