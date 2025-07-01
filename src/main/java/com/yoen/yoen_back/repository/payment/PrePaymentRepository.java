package com.yoen.yoen_back.repository.payment;

import com.yoen.yoen_back.entity.payment.PrePayment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrePaymentRepository extends JpaRepository<PrePayment, Long> {
}
