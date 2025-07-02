package com.yoen.yoen_back.controller;


import com.yoen.yoen_back.dto.ApiResponse;
import com.yoen.yoen_back.entity.user.Role;
import com.yoen.yoen_back.service.ConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/config")
public class ConfigController {
    private final ConfigService configService;

    @PostMapping("/setRoles")
    public ResponseEntity<ApiResponse<List<Role>>> setRoles(@RequestBody List<Role> roles) {
        try {
            List<Role> troles = configService.saveRoles(roles);
            return ResponseEntity.ok(ApiResponse.success(troles));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.failure(e.getMessage()));
        }
    }
}
