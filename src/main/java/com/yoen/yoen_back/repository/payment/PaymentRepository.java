package com.yoen.yoen_back.repository.payment;

import com.yoen.yoen_back.entity.payment.Payment;
import com.yoen.yoen_back.entity.travel.Travel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByTravel_TravelIdAndIsActiveTrue(Long travelId);

    List<Payment> findAllByTravelAndPayTimeBetween(Travel tv, LocalDateTime localDateTime, LocalDateTime localDateTime1);

    List<Payment> findByTravelAndIsActiveTrue(Travel travel);

    Optional<Payment> findByPaymentIdAndIsActiveTrue(Long paymentId);
}
