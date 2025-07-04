package com.yoen.yoen_back.controller;

import com.yoen.yoen_back.common.security.CustomUserDetails;
import com.yoen.yoen_back.dto.ApiResponse;
import com.yoen.yoen_back.service.PhotoUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/photo")
public class PhotoController {
    private final PhotoUploadService photoUploadService;
    @PostMapping(path = "/upload")
    public ResponseEntity<ApiResponse<String>> upload(@AuthenticationPrincipal CustomUserDetails userDetail, @RequestPart("image") MultipartFile file) {
        String url = photoUploadService.upload(userDetail.user(), file);
        return ResponseEntity.ok(ApiResponse.success(url));
    }
}
