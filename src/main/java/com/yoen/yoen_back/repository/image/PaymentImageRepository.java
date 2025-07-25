package com.yoen.yoen_back.repository.image;

import com.yoen.yoen_back.entity.image.PaymentImage;
import com.yoen.yoen_back.entity.payment.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentImageRepository extends JpaRepository<PaymentImage, Long> {
    Optional<PaymentImage> findByPaymentImageIdAndIsActiveTrue(Long id);
    List<PaymentImage> findByPayment_PaymentId(Long id);

    List<PaymentImage> findByPayment(Payment pm);
}
