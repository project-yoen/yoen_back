package com.yoen.yoen_back.repository.jpa.payment;

import com.yoen.yoen_back.entity.payment.PaymentSettlement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentSettlementRepository extends JpaRepository<PaymentSettlement, Long> {
}
