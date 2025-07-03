package com.yoen.yoen_back.repository.jpa.image;

import com.yoen.yoen_back.entity.image.Image;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageRepository extends JpaRepository<Image, Integer> {
}
