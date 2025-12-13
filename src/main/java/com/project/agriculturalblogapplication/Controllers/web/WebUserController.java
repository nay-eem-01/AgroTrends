package com.project.agriculturalblogapplication.Controllers.web;

import com.project.agriculturalblogapplication.DTOS.UserDto;
import com.project.agriculturalblogapplication.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("app/users/")
public class WebUserController {

    private final UserService userService;

    public WebUserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("register")
    public ResponseEntity<UserDto> addNewUser(@RequestBody UserDto userDto){
        UserDto savedUserDto = userService.addNewAuthor(userDto);
        return new ResponseEntity<>(savedUserDto,HttpStatus.OK);
    }
}
