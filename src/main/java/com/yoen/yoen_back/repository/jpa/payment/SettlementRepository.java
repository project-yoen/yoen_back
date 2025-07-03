package com.yoen.yoen_back.repository.jpa.payment;

import com.yoen.yoen_back.entity.payment.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementRepository extends JpaRepository<Settlement, Integer> {
}
