package com.yoen.yoen_back.repository.image;

import com.yoen.yoen_back.entity.image.PaymentImage;
import com.yoen.yoen_back.entity.payment.Payment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentImageRepository extends JpaRepository<PaymentImage, Long> {
    Optional<PaymentImage> findByPaymentImageIdAndIsActiveTrue(Long id);
    List<PaymentImage> findByPayment_PaymentId(Long id);

    // 상세 조회에서 image.imageUrl을 쓰므로 N+1 방지용 fetch
    @EntityGraph(attributePaths = {"image"})
    List<PaymentImage> findByPayment(Payment pm);
}
