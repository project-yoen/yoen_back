package com.yoen.yoen_back.repository.payment;

import com.yoen.yoen_back.entity.payment.Payment;
import com.yoen.yoen_back.entity.payment.Settlement;
import com.yoen.yoen_back.entity.travel.Travel;
import com.yoen.yoen_back.enums.PaymentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    Optional<Settlement> findBySettlementIdAndIsActiveTrue(Long settlementId);
    List<Settlement> findByPayment_PaymentIdAndIsActiveTrue(Long paymentId);

    List<Settlement> findByPaymentAndIsActiveTrue(Payment payment);

    @Query("""
    SELECT s
    FROM Settlement s
    WHERE s.payment.travel = :travel
      AND s.payment.type IN :paymentTypes
      AND s.payment.payTime BETWEEN :startAt AND :endAt
      AND s.isActive = true
""")
    List<Settlement> findSettlementByOptions(
            @Param("travel") Travel tv,
            @Param("paymentTypes") List<PaymentType> paymentTypes,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt
    );
}
