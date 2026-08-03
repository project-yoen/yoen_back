package com.yoen.yoen_back.repository.payment;

import com.yoen.yoen_back.entity.payment.Payment;
import com.yoen.yoen_back.entity.travel.Travel;
import com.yoen.yoen_back.enums.PaymentType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByTravel_TravelIdAndIsActiveTrue(Long travelId);

    // 목록 DTO 변환에서 category.categoryName, travelUser.travelNickname을 쓰므로 N+1 방지용 fetch
    @EntityGraph(attributePaths = {"category", "travelUser"})
    List<Payment> findAllByTravelAndTypeAndPayTimeBetweenAndIsActiveTrue(Travel tv, PaymentType paymentType, LocalDateTime localDateTime, LocalDateTime localDateTime1);

    List<Payment> findByTravelAndIsActiveTrue(Travel travel);

    Optional<Payment> findByPaymentIdAndIsActiveTrue(Long paymentId);

    @EntityGraph(attributePaths = {"category", "travelUser"})
    List<Payment> findAllByTravelAndTypeAndIsActiveTrue(Travel tv, PaymentType paymentType);

    @EntityGraph(attributePaths = {"category", "travelUser"})
    List<Payment> findAllByTravelAndTypeInAndPayTimeBetweenAndIsActiveTrue(
            Travel tv,
            Collection<PaymentType> types,
            LocalDateTime localDateTime,
            LocalDateTime localDateTime1
    );

    // 상세 조회용: 응답 DTO가 travel/category/결제자(travelUser→user→프로필이미지)까지 접근하므로 한 번에 fetch
    @EntityGraph(attributePaths = {"travel", "category", "travelUser", "travelUser.user", "travelUser.user.profileImage"})
    Optional<Payment> findWithDetailByPaymentIdAndIsActiveTrue(Long paymentId);
}
