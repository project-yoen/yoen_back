package com.yoen.yoen_back.repository.payment;

import com.yoen.yoen_back.entity.payment.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    Optional<Settlement> findBySettlementIdAndIsActiveTrue(Long settlementId);
}
