package com.project.agriculturalblogapplication.Util;


import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.Repositories.UserRepository;
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
}
