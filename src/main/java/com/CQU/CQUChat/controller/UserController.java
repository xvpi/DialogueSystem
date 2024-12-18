package com.CQU.CQUChat.controller;

import com.CQU.CQUChat.entity.User;
import com.CQU.CQUChat.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {
    @Autowired
    private UserMapper mapper;

    @GetMapping("/signup")
    public int signup(String nickname, String password){
        return mapper.signup(nickname, password);
    }

    @GetMapping("/login")
    public User login(String nickname, String password){
        return mapper.login(nickname, password);
    }
}
