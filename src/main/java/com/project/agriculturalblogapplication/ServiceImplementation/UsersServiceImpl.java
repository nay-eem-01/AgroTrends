package com.project.agriculturalblogapplication.ServiceImplementation;

import com.project.agriculturalblogapplication.DTOS.UserDto;
import com.project.agriculturalblogapplication.ExceptionHandler.APIExceptionHandler;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.Repositories.UserRepository;
import com.project.agriculturalblogapplication.Service.UsersService;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UsersServiceImpl implements UsersService {
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    public UsersServiceImpl(UserRepository userRepository, ModelMapper modelMapper) {
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    public UserDto addNewAuthor(UserDto userDto) {
        Optional<User> existingAuthor = userRepository.findByEmail(userDto.getUsersEmail());
        if (existingAuthor.isPresent()){
            throw new APIExceptionHandler("User Already exists");
        }
        User savedUser = userRepository.save(modelMapper.map(userDto, User.class));
        return modelMapper.map(savedUser,UserDto.class);
    }
}
