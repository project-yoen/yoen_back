package com.yoen.yoen_back.controller;

import com.yoen.yoen_back.common.entity.ApiResponse;
import com.yoen.yoen_back.dto.user.LoginRequestDto;
import com.yoen.yoen_back.dto.user.LoginResponseDto;
import com.yoen.yoen_back.dto.user.RegisterRequestDto;
import com.yoen.yoen_back.service.AuthService;
import com.yoen.yoen_back.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.apache.http.auth.InvalidCredentialsException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin("*")
@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> signUp(@RequestBody RegisterRequestDto dto) {
        userService.register(dto);

        return ResponseEntity.ok(ApiResponse.success("Sign Up Success"));
    }


    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(@RequestBody LoginRequestDto dto) throws InvalidCredentialsException {
        LoginResponseDto lgd = authService.loginAndGetToken(dto);
        return ResponseEntity.ok(ApiResponse.success(lgd));
    }

    @GetMapping("/exists")
    public ResponseEntity<ApiResponse<Boolean>> existEmail(@RequestParam("email") String email) {
        return ResponseEntity.ok(ApiResponse.success(userService.validateEmail(email)));
    }
}
