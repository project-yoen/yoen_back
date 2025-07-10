package com.yoen.yoen_back.controller;

import com.yoen.yoen_back.dto.ApiResponse;
import com.yoen.yoen_back.dto.RefreshTokenRequestDto;
import com.yoen.yoen_back.dto.TokenResponse;
import com.yoen.yoen_back.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    //todo : 리프레시, 액세스 토큰 만료시 재발급?
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(@RequestBody RefreshTokenRequestDto refreshTokenRequest) {
        String refreshToken = refreshTokenRequest.refreshToken();
        return ResponseEntity.ok(ApiResponse.success(authService.reissueTokens(refreshToken)));

    }



}
