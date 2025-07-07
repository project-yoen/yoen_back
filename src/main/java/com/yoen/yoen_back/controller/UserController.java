package com.yoen.yoen_back.controller;

import com.yoen.yoen_back.common.infrastructure.JwtProvider;
import com.yoen.yoen_back.dto.ApiResponse;
import com.yoen.yoen_back.dto.LoginRequestDto;
import com.yoen.yoen_back.dto.LoginResponseDto;
import com.yoen.yoen_back.dto.RegisterRequestDto;
import com.yoen.yoen_back.entity.user.User;
import com.yoen.yoen_back.service.AuthService;
import com.yoen.yoen_back.service.UserService;
import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.auth.InvalidCredentialsException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin("*")
@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    // 성공시 유저 정보와 토큰 반환
//    @PostMapping("/signIn")
//    public ResponseEntity<?> signIn(@RequestParam String email, @RequestParam String password) {}


    @GetMapping("/getAllUser")
    public ResponseEntity<ApiResponse<List<User>>> test() {
        return ResponseEntity.ok(ApiResponse.success(userService.test()));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> signUp(@RequestBody RegisterRequestDto dto) {
        User newUser = userService.register(dto);

        // 토큰 발급 (액세스, 리프레시)
        return ResponseEntity.ok(ApiResponse.success("Sign Up Success"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(@RequestBody LoginRequestDto dto) throws InvalidCredentialsException {
        LoginResponseDto lgd = authService.loginAndGetToken(dto);
        return ResponseEntity.ok(ApiResponse.success(lgd));
    }

}
