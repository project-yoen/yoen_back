package com.yoen.yoen_back.repository.payment;

import com.yoen.yoen_back.entity.payment.Settlement;
import com.yoen.yoen_back.entity.payment.SettlementUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SettlementUserRepository extends JpaRepository<SettlementUser, Long> {
    List<SettlementUser> findAllBySettlement_SettlementId(Long settlementId);

    List<SettlementUser> findBySettlement(Settlement settlement);
}
