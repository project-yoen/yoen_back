package com.yoen.yoen_back.controller;

import com.yoen.yoen_back.common.entity.ApiResponse;
import com.yoen.yoen_back.common.security.CustomUserDetails;
import com.yoen.yoen_back.dto.etc.IdListRequest;
import com.yoen.yoen_back.entity.image.Image;
import com.yoen.yoen_back.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/image")
public class ImageController {
    private final ImageService imageService;

    @PostMapping(path = "/create")
    public ResponseEntity<ApiResponse<String>> upload(@AuthenticationPrincipal CustomUserDetails userDetail, @RequestPart("image") MultipartFile file) {
        Image uploadedImage = imageService.saveImage(userDetail.user(), file);
        return ResponseEntity.ok(ApiResponse.success(uploadedImage.getImageUrl()));
    }


    @PostMapping(path = "/multiple/create")
    public ResponseEntity<ApiResponse<List<String>>> uploadMultiple(
            @AuthenticationPrincipal CustomUserDetails userDetail,
            @RequestPart("images") List<MultipartFile> files) {

        List<Image> uploadedImages = imageService.saveImages(userDetail.user(), files);

        // 일단 urls 예시
        List<String> urls = uploadedImages.stream().map(Image::getImageUrl).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(urls));
    }

    @DeleteMapping(path = "/delete/{imageId}")
    public ResponseEntity<ApiResponse<String>> deleteSingle(
            @PathVariable Long imageId) {
        String removedImageUrl = imageService.deleteImage(imageId);

        return ResponseEntity.ok(ApiResponse.success("The image has been successfully deleted: " + removedImageUrl));
    }

    @DeleteMapping(path = "/multiple/delete")
    public ResponseEntity<ApiResponse<String>> deleteMultiple(
            @RequestBody IdListRequest idListRequest
    ) {

        List<String> removedImageUrls = imageService.deleteImages(idListRequest.ids());
        String urls = String.join(",", removedImageUrls);

        return ResponseEntity.ok(ApiResponse.success("The image has been successfully deleted: " + urls));
    }


}
