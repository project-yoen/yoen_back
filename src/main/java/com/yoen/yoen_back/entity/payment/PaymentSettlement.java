package com.yoen.yoen_back.entity.payment;

import com.yoen.yoen_back.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 결제 정산 기록 엔티티
 * 결제 정산 기록의 중간자 테이블로 결제에 대한 정산 기록들을 매핑
 */

@Getter
@Setter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "paymentsettlements")
public class PaymentSettlement extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long patmentSettlementId;

    @JoinColumn(name = "payment_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Payment payment;

    @JoinColumn(name = "settlement_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Settlement settlement;
}
