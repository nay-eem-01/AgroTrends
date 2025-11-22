package com.project.agriculturalblogapplication.Controllers.web;

import com.project.agriculturalblogapplication.DTOS.UserDto;
import com.project.agriculturalblogapplication.Service.UsersService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("app/users/")
public class UserController {

    private final UsersService usersService ;

    public UserController(UsersService usersService) {
        this.usersService = usersService;
    }

    @PostMapping("register")
    public ResponseEntity<UserDto> addNewUser(@RequestBody UserDto userDto){
        UserDto savedUserDto = usersService.addNewAuthor(userDto);
        return new ResponseEntity<>(savedUserDto,HttpStatus.OK);
    }
}
