package com.project.agriculturalblogapplication.ServiceImplementation;

import com.project.agriculturalblogapplication.DTOS.UserDto;
import com.project.agriculturalblogapplication.ExceptionHandler.APIExceptionHandler;
import com.project.agriculturalblogapplication.Models.Users;
import com.project.agriculturalblogapplication.Repositories.UserRepositories;
import com.project.agriculturalblogapplication.Service.UsersService;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UsersServiceImpl implements UsersService {
    private final UserRepositories userRepositories;
    private final ModelMapper modelMapper;

    public UsersServiceImpl(UserRepositories userRepositories, ModelMapper modelMapper) {
        this.userRepositories = userRepositories;
        this.modelMapper = modelMapper;
    }

    @Override
    public UserDto addNewAuthor(UserDto userDto) {
        Optional<Users> existingAuthor = userRepositories.findByUserEmail(userDto.getUsersEmail());
        if (existingAuthor.isPresent()){
            throw new APIExceptionHandler("User Already exists");
        }
        Users savedUser = userRepositories.save(modelMapper.map(userDto,Users.class));
        return modelMapper.map(savedUser,UserDto.class);
    }
}
