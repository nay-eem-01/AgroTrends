package com.project.agriculturalblogapplication.security;

import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.repositories.UserRepository;
import com.project.agriculturalblogapplication.security.service.CustomUserDetailService;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Sign-in accepts an e-mail or a phone number; the token subject (and so the username) is the user id. */
class CustomUserDetailServiceTest {

    private final UserRepository users = mock(UserRepository.class);
    private final CustomUserDetailService service = new CustomUserDetailService(users);

    private static User user() {
        User user = new User();
        user.setId(7L);
        user.setRoles(Set.of());
        return user;
    }

    @Test
    void findsTheAccountByEmail() {
        when(users.findTopByEmailEqualsIgnoreCase("rahim@example.com")).thenReturn(Optional.of(user()));

        assertEquals("7", service.loadUserByUsername("rahim@example.com").getUsername());
    }

    @Test
    void findsTheAccountByPhoneInAnyAcceptedForm() {
        when(users.findTopByMobileNumber("+8801712345678")).thenReturn(Optional.of(user()));

        assertEquals("7", service.loadUserByUsername("01712345678").getUsername());
        assertEquals("7", service.loadUserByUsername("+8801712345678").getUsername());
    }

    @Test
    void aConfirmedPhoneAloneCountsAsConfirmed() {
        User user = user();
        user.setPhoneVerifiedAt(java.time.LocalDateTime.now());
        when(users.findById(7L)).thenReturn(Optional.of(user));

        assertTrue(service.loadUserById(7L).isContactVerified());
    }

    @Test
    void somethingThatIsNeitherIsUnknown() {
        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("not a phone"));
    }

    @Test
    void loadsTheAccountATokenNames() {
        when(users.findById(7L)).thenReturn(Optional.of(user()));

        assertEquals(7L, service.loadUserById(7L).getId());
        assertThrows(UsernameNotFoundException.class, () -> service.loadUserById(8L));
    }
}
