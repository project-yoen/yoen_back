package com.yoen.yoen_back.controller;

import com.yoen.yoen_back.common.entity.ApiResponse;
import com.yoen.yoen_back.common.security.CustomUserDetails;
import com.yoen.yoen_back.dto.user.*;
import com.yoen.yoen_back.service.AuthService;
import com.yoen.yoen_back.service.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.apache.http.auth.InvalidCredentialsException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "JWT_AUTH")
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> signUp(@RequestBody RegisterRequestDto dto) {
        userService.register(dto);

        return ResponseEntity.ok(ApiResponse.success("Sign Up Success"));
    }

    @PostMapping("/update")
    public ResponseEntity<ApiResponse<UserResponseDto>> updateUser(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestBody UpdateUserDto dto) {

        return ResponseEntity.ok(ApiResponse.success(userService.updateUser(userDetails.user(), dto)));
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponseDto>> signUp(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(userService.findUserResponseById(userDetails.user().getUserId())));
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

    @PostMapping("/profileImage")
    public ResponseEntity<ApiResponse<String>> setProfileImage(@AuthenticationPrincipal CustomUserDetails userDetails, MultipartFile profileImage) {
        return ResponseEntity.ok(ApiResponse.success(userService.saveProfileUrl(userDetails.user(), profileImage)));
    }
}
