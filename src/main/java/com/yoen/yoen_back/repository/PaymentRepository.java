package com.yoen.yoen_back.repository;

import com.yoen.yoen_back.entity.payment.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
