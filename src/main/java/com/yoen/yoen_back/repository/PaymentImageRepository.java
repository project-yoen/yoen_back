package com.yoen.yoen_back.repository;

import com.yoen.yoen_back.entity.image.PaymentImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentImageRepository extends JpaRepository<PaymentImage, Long> {
}
