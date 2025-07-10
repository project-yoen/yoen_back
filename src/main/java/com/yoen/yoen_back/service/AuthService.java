package com.yoen.yoen_back.service;

import com.yoen.yoen_back.common.infrastructure.JwtProvider;
import com.yoen.yoen_back.dto.LoginRequestDto;
import com.yoen.yoen_back.dto.LoginResponseDto;
import com.yoen.yoen_back.entity.user.User;
import lombok.RequiredArgsConstructor;
import org.apache.http.auth.InvalidCredentialsException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final JwtProvider jwtProvider;
    private final UserService userService;

    public String generateRefreshToken(Long userId) {
        return jwtProvider.generateRefreshToken(String.valueOf(userId));
    }

    public String generateAccessToken(Long userId) {
        return jwtProvider.generateAccessToken(String.valueOf(userId));
    }

    public LoginResponseDto loginAndGetToken(LoginRequestDto dto) throws InvalidCredentialsException {
        User user = userService.login(dto);
        Long userId = user.getUserId();

        return new LoginResponseDto(user, generateAccessToken(userId), generateRefreshToken(userId));
    }
}
