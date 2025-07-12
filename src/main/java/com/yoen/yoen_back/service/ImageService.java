package com.yoen.yoen_back.service;

import com.yoen.yoen_back.dto.ImageResponseDto;
import com.yoen.yoen_back.dto.UploadedImage;
import com.yoen.yoen_back.entity.image.Image;
import com.yoen.yoen_back.entity.user.User;
import com.yoen.yoen_back.repository.image.ImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ImageService {
    private final ImageRepository imageRepository;
    private final ImageUploadService imageUploadService;

    public Image saveImage(User user, MultipartFile file) {
        UploadedImage uploadedImage = imageUploadService.uploadImage(user, file);

        Image image = Image.builder()
                .imageUrl(uploadedImage.imageUrl())
                .objectKey(uploadedImage.objectKey())
                .user(user)
                .build();
        return imageRepository.save(image);
    }

    public List<Image> saveImages(User user, List<MultipartFile> files) {
        List<UploadedImage> uploadedImages = imageUploadService.uploadImages(user, files);

        List<Image> images = uploadedImages.stream().map((uploaedImage) -> {
            return Image.builder().imageUrl(uploaedImage.imageUrl()).objectKey(uploaedImage.objectKey()).user(user).build();
        }).collect(Collectors.toList());

        return imageRepository.saveAll(images);
    }


    // imageId를 받아 삭제
    public String deleteImage(Long imageId) {
        Optional<Image> optionalImage = imageRepository.findByImageIdAndIsActiveTrue(imageId);
        optionalImage.ifPresent(image -> {
            // 클라우드에서 삭제
            imageUploadService.delete(image.getObjectKey());
            // 로컿DB에서 소프트 삭제
            softDeleteImage(imageId);
        });

        return optionalImage.map(Image::getImageUrl).orElse(null);
    }

    // imageId 리스트를 받아 삭제
    public List<String> deleteImages(List<Long> imageIds) {
        List<String> results = new ArrayList<>();

        imageIds.forEach(imageId -> {
            String url = deleteImage(imageId);
            results.add(url);
        });
        return results;
    }

    // 소프트 삭제
    private void softDeleteImage(Long imageId) {
        Optional<Image> optionalImage = imageRepository.findByImageIdAndIsActiveTrue(imageId);
        optionalImage.ifPresent(image -> {
            image.setIsActive(false);
            imageRepository.save(image);
        });
    }
}
