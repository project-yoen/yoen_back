package com.yoen.yoen_back.entity.payment;

import com.yoen.yoen_back.common.entity.BaseEntity;
import com.yoen.yoen_back.entity.travel.Travel;
import com.yoen.yoen_back.entity.travel.TravelUser;
import com.yoen.yoen_back.enums.PaymentMethod;
import jakarta.persistence.*;
import lombok.*;

/** 정산 엔티티
 */
@Getter
@Setter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(name = "settlements")
public class Settlement extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long settlementId;

    @JoinColumn(name = "payment_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Payment payment;

    private String settlementName;

    private Long amount;


}
