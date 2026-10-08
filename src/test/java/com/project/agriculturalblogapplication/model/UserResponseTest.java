package com.project.agriculturalblogapplication.model;

import com.project.agriculturalblogapplication.entities.Role;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.model.response.PublicUserResponse;
import com.project.agriculturalblogapplication.model.response.UserResponse;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserResponseTest {

    @Test
    void ownAccountShowsContactDetailsAndRoleNamesButNoPassword() {
        UserResponse response = UserResponse.from(user(), 20L);

        assertEquals("rahim@example.com", response.email());
        assertEquals(20L, response.authorId());
        assertNull(UserResponse.from(user(), null).authorId());
        assertEquals(Set.of("ROLE_USER"), response.roles());
        assertTrue(Arrays.stream(UserResponse.class.getRecordComponents())
                .noneMatch(c -> c.getName().toLowerCase().contains("password") && c.getType() == String.class));
    }

    @Test
    void publicProfileShowsOnlyIdAndName() {
        // Regression: /api/user/id/{id} returned the whole User (e-mail, mobile, roles) to any signed-in user.
        assertEquals(new PublicUserResponse(10L, "Rahim"), PublicUserResponse.from(user()));
        assertEquals(2, PublicUserResponse.class.getRecordComponents().length);
    }

    private static User user() {
        Role role = new Role();
        role.setRoleName("ROLE_USER");
        User user = new User();
        user.setId(10L);
        user.setName("Rahim");
        user.setEmail("rahim@example.com");
        user.setMobileNumber("+8801700000000");
        user.setPassword("$2a$10$hash");
        user.setRoles(Set.of(role));
        return user;
    }
}
