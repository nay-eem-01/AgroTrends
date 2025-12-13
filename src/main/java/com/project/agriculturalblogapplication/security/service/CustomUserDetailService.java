package com.project.agriculturalblogapplication.security.service;

import com.project.agriculturalblogapplication.Repositories.UserRepository;
import com.project.agriculturalblogapplication.entities.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailService implements UserDetailsService {
    private final UserRepository userRepository;

    public CustomUserDetailService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User loggedUser = userRepository.findTopByEmail(email).orElseThrow(()-> new UsernameNotFoundException("User not found with email" + email));
        return CustomUserDetails.build(loggedUser);
    }
}
