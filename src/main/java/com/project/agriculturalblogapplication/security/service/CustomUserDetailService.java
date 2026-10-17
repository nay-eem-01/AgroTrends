package com.project.agriculturalblogapplication.security.service;

import com.project.agriculturalblogapplication.repositories.UserRepository;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.util.PhoneNumbers;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailService implements UserDetailsService {

    private final UserRepository userRepository;

    /** Sign-in lookup: the identifier is an e-mail address or a phone number. */
    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        return CustomUserDetails.build(findBySignInIdentifier(identifier)
                .orElseThrow(() -> new UsernameNotFoundException("User not found.")));
    }

    /** The user an access token names: its subject is the user id. */
    public CustomUserDetails loadUserById(Long userId) {
        return CustomUserDetails.build(userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found.")));
    }

    private Optional<User> findBySignInIdentifier(String identifier) {
        if (identifier.contains("@")) {
            return userRepository.findTopByEmailEqualsIgnoreCase(identifier);
        }
        String phone = PhoneNumbers.normalise(identifier);
        return phone == null ? Optional.empty() : userRepository.findTopByMobileNumber(phone);
    }
}
