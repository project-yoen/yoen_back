package com.yoen.yoen_back.controller;

import com.yoen.yoen_back.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    //todo : 리프레시, 액세스 토큰 만료시 재발급?
    @PostMapping


}
