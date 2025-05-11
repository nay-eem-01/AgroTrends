package com.project.agriculturalblogapplication.Util;


import com.project.agriculturalblogapplication.Models.Users;
import com.project.agriculturalblogapplication.Repositories.UserRepositories;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class AuthUtil {

    private final UserRepositories userRepository;

    public AuthUtil(UserRepositories userRepository) {
        this.userRepository = userRepository;
    }

    public String LoggedInEmail(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Users user = userRepository.findByUserName(authentication.getName())
                .orElseThrow(()-> new UsernameNotFoundException("User not found with username " + authentication.getName()));

        return user.getUserEmail();
    }
    public Long LoggedInUserId(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Users user = userRepository.findByUserName(authentication.getName())
                .orElseThrow(()-> new UsernameNotFoundException("User not found with username " + authentication.getName()));

        return user.getUserId();
    }
    public Users LoggedInUser(){

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        return userRepository.findByUserName(authentication.getName())
                .orElseThrow(()-> new UsernameNotFoundException("User not found with username " + authentication.getName()));
    }
}
