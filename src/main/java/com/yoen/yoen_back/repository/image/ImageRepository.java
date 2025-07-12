package com.yoen.yoen_back.repository.image;

import com.yoen.yoen_back.entity.image.Image;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ImageRepository extends JpaRepository<Image, Long> {
    Optional<Image> findByImageIdAndIsActiveTrue(Long imageId);
}
