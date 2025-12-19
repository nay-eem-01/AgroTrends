package com.project.agriculturalblogapplication.util;


import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.repositories.UserRepository;
import com.project.agriculturalblogapplication.security.service.CustomUserDetails;
import com.project.agriculturalblogapplication.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class AuthUtil {

    private final UserRepository userRepository;

    public AuthUtil(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String LoggedInEmail(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByName(authentication.getName())
                .orElseThrow(()-> new UsernameNotFoundException("User not found with username " + authentication.getName()));

        return user.getEmail();
    }
    public Long LoggedInUserId(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByName(authentication.getName())
                .orElseThrow(()-> new UsernameNotFoundException("User not found with username " + authentication.getName()));

        return user.getId();
    }
    public User LoggedInUser(){

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        return userRepository.findByName(authentication.getName())
                .orElseThrow(()-> new UsernameNotFoundException("User not found with username " + authentication.getName()));
    }

    public static User getLoggedInUser(UserService userService) {
        User user;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if(authentication == null) return null;

        try {
            Object principal = authentication.getPrincipal();

            if (principal instanceof CustomUserDetails) {
                CustomUserDetails customUserDetails = (CustomUserDetails) principal;

                user = new User();
                user.setId(customUserDetails.getId());
                user.setEmail(customUserDetails.getEmail());
                user.setRoles(customUserDetails.getRoles());
                user.setPassword(customUserDetails.getPassword());
            } else {
                user = userService.findByEmail(authentication.getName());
            }
        } catch(Exception e) {
            user = userService.findByEmail(authentication.getName());
        }

        return user;
    }
}
