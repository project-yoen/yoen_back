package com.yoen.yoen_back.service;

import com.yoen.yoen_back.dto.etc.image.UploadedImage;
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

    // image저장하는 함수 (imageUploadService를 통해 클라우드에 업로드후 url을 로컬에 저장)
    public Image saveImage(User user, MultipartFile file) {
        UploadedImage uploadedImage = imageUploadService.uploadImage(user, file);

        Image image = Image.builder()
                .imageUrl(uploadedImage.imageUrl())
                .objectKey(uploadedImage.objectKey())
                .user(user)
                .build();
        return imageRepository.save(image);
    }

    public Optional<Image> getImageById(Long imageId) {
        return imageRepository.findByImageIdAndIsActiveTrue(imageId);
    }

    // image 여러개를 한번에 클라우드에 저장하고 로컬DB에 저장하는 함수
    public List<Image> saveImages(User user, List<MultipartFile> files) {
        List<UploadedImage> uploadedImages = imageUploadService.uploadImages(user, files);

        List<Image> images = uploadedImages.stream().map((uploaedImage) -> Image.builder().imageUrl(uploaedImage.imageUrl()).objectKey(uploaedImage.objectKey()).user(user).build()).collect(Collectors.toList());

        return imageRepository.saveAll(images);
    }


    // imageId를 받아 삭제하는 함수
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

    // imageId 리스트를 받아 삭제하는 함수
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
