package com.yoen.yoen_back.repository.payment;

import com.yoen.yoen_back.entity.payment.Payment;
import com.yoen.yoen_back.entity.payment.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    Optional<Settlement> findBySettlementIdAndIsActiveTrue(Long settlementId);
    List<Settlement> findByPayment_PaymentIdAndIsActiveTrue(Long paymentId);

    List<Settlement> findByPaymentAndIsActiveTrue(Payment payment);
}
