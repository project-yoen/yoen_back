package com.yoen.yoen_back.service;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.yoen.yoen_back.dto.etc.image.UploadedImage;
import com.yoen.yoen_back.entity.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ImageUploadService {

    private final Bucket bucket;

    // 클라우드에 이미지 업로드 하는 함수 objectKey와 imageUrl을 반환
    public UploadedImage uploadImage(User user, MultipartFile file) {
        try {
            String ext = Optional.ofNullable(file.getOriginalFilename())
                    .map(name -> name.substring(name.lastIndexOf('.') + 1))
                    .orElse("jpg");

            String objectKey = "images/user_" + user.getUserId() + "/" + UUID.randomUUID() + "." + ext;

            // 스토리지 저장 코드
            Blob blob = bucket.create(
                    objectKey,
                    file.getBytes(),
                    file.getContentType() != null ? file.getContentType() : "image/jpeg"
            );

            String token = UUID.randomUUID().toString();

            // 스토리지에 토큰 설정
            blob.toBuilder()
                    .setMetadata(Map.of("firebaseStorageDownloadTokens", token))
                    .build()
                    .update();

            String imageUrl = getImageUrl(blob.getName(), token);
            return new UploadedImage(objectKey, imageUrl);

        } catch (IOException e) {
            throw new RuntimeException("사진 업로드 실패", e);
        }
    }

    public UploadedImage copyImage(User user, String sourceObjectKey) {
        Blob sourceBlob = bucket.get(sourceObjectKey);
        if (sourceBlob == null) {
            throw new IllegalStateException("복사할 이미지가 존재하지 않습니다.");
        }

        String ext = extractExtension(sourceObjectKey);
        String objectKey = "images/user_" + user.getUserId() + "/" + UUID.randomUUID() + "." + ext;
        Blob copiedBlob = sourceBlob.copyTo(bucket.getName(), objectKey).getResult();
        String token = UUID.randomUUID().toString();

        copiedBlob.toBuilder()
                .setMetadata(Map.of("firebaseStorageDownloadTokens", token))
                .build()
                .update();

        return new UploadedImage(objectKey, getImageUrl(objectKey, token));
    }

    // 여러 이미지 업로드 처리하는 함수
    public List<UploadedImage> uploadImages(User user, List<MultipartFile> files) {
        List<UploadedImage> results = new ArrayList<>();

        for (MultipartFile file : files) {
            results.add(uploadImage(user, file)); // 기존 단일 업로드 재사용
        }

        return results;
    }


    // objectKey로 업로드된 이미지 삭제하는 함수
    public void delete(String objectKey) {
        boolean deleted = bucket.get(objectKey).delete();

        if (!deleted) {
            throw new RuntimeException("파일 삭제 실패: " + objectKey);
        }
    }

    private String extractExtension(String objectKey) {
        int dotIndex = objectKey.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == objectKey.length() - 1) {
            return "jpg";
        }
        return objectKey.substring(dotIndex + 1);
    }

    private String getImageUrl(String objectKey, String token) {
        String encodedName = URLEncoder.encode(objectKey, StandardCharsets.UTF_8);
        return String.format("https://firebasestorage.googleapis.com/v0/b/%s/o/%s?alt=media&token=%s",
                bucket.getName(), encodedName, token);
    }

}
