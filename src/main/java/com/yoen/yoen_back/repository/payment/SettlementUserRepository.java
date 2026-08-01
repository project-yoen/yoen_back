package com.yoen.yoen_back.repository.payment;

import com.yoen.yoen_back.entity.payment.Settlement;
import com.yoen.yoen_back.entity.payment.SettlementUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface SettlementUserRepository extends JpaRepository<SettlementUser, Long> {
    List<SettlementUser> findAllBySettlementAndIsActiveTrue(Settlement settlement);

    List<SettlementUser> findBySettlementAndIsActiveTrue(Settlement settlement);

    // 결제 상세: 정산별 개별 조회(N+1) 대신 paymentId 기준으로 정산유저+travelUser를 한 번에 fetch
    @Query("""
    SELECT su
    FROM SettlementUser su
    JOIN FETCH su.settlement s
    LEFT JOIN FETCH su.travelUser tu
    WHERE s.payment.paymentId = :paymentId
      AND s.isActive = true
      AND su.isActive = true
    """)
    List<SettlementUser> findAllWithTravelUserByPaymentId(@Param("paymentId") Long paymentId);

    // 정산 화면: 정산 리스트 전체의 정산유저를 한 번에 fetch (정산별 개별 조회 N+1 제거)
    @Query("""
    SELECT su
    FROM SettlementUser su
    LEFT JOIN FETCH su.travelUser tu
    WHERE su.settlement IN :settlements
      AND su.isActive = true
    """)
    List<SettlementUser> findAllWithTravelUserBySettlementIn(@Param("settlements") Collection<Settlement> settlements);
}
