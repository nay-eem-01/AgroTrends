package com.project.agriculturalblogapplication.Service;

import com.project.agriculturalblogapplication.DTOS.UserDto;
import com.project.agriculturalblogapplication.Models.Users;
import org.springframework.stereotype.Service;

@Service
public interface UsersService {
    UserDto addNewAuthor(UserDto userDto);
}
