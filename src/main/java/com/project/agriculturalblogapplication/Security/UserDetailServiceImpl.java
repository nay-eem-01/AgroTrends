package com.project.agriculturalblogapplication.Security;


import com.project.agriculturalblogapplication.Models.Users;
import com.project.agriculturalblogapplication.Repositories.UserRepositories;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailServiceImpl implements UserDetailsService {

    private final UserRepositories userRepository;

    public UserDetailServiceImpl(UserRepositories userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        Users user = userRepository.findByUserName(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));


        return UserDetailsImpl.build(user);
    }

}
