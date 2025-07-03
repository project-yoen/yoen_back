package com.yoen.yoen_back.repository.payment;

import com.yoen.yoen_back.entity.payment.SettlementUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementUserRepository extends JpaRepository<SettlementUser, Long> {
}
