package com.yoen.yoen_back.controller;

import com.yoen.yoen_back.entity.user.User;
import com.yoen.yoen_back.repository.user.UserRepository;
import com.yoen.yoen_back.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    // 성공시 유저 정보와 토큰 반환
//    @PostMapping("/signIn")
//    public ResponseEntity<?> signIn(@RequestParam String email, @RequestParam String password) {}


    @GetMapping("/test")
    public ResponseEntity<List<User>> test() {
        return ResponseEntity.ok(userService.test());
    }

    @PostMapping("/signUp")
    public ResponseEntity<?> signUp(@RequestBody User user) {
        try {
            userService.signUp(user);
            return ResponseEntity.ok("Sign Up Success");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
