package com.yoen.yoen_back.entity.payment;

import com.yoen.yoen_back.common.entity.BaseEntity;
import com.yoen.yoen_back.entity.Category;
import com.yoen.yoen_back.entity.travel.Travel;
import jakarta.persistence.*;
import lombok.*;


/** 사전사용금액 엔티티
 */
@Getter
@Setter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(name = "prepayments")
public class PrePayment extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long prePaymentId;

    @JoinColumn(name = "travel_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Travel travel;

    @JoinColumn(name = "category_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Category category;

    @JoinColumn(name = "settlement_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Settlement settlement;
}
