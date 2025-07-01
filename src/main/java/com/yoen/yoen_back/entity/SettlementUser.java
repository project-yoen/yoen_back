package com.yoen.yoen_back.entity;

import com.yoen.yoen_back.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** 정산 유저 엔티티
 *  각 유저가 관련되어 있는 정산 기록과 매핑
 *  정정사항이 생길경우 기존 settlement와 매핑된 레코드는 비활성화후 새로 작성
 */
@Getter
@Setter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "settlementusers")
public class SettlementUser extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long settlementUserId;

    @JoinColumn(name = "settlement_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Settlement settlement;

    @JoinColumn(name = "traveluser_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private TravelUser travelUser;

    @Column(nullable = false)
    private Long amount;

    private LocalDateTime paidAt;
    private Boolean isPaid = false;
}
