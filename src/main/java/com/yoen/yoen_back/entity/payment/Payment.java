package com.yoen.yoen_back.entity.payment;

import com.yoen.yoen_back.common.entity.BaseEntity;
import com.yoen.yoen_back.entity.Category;
import com.yoen.yoen_back.entity.travel.Travel;
import com.yoen.yoen_back.entity.travel.TravelUser;
import com.yoen.yoen_back.enums.Currency;
import com.yoen.yoen_back.enums.Payer;
import com.yoen.yoen_back.enums.PaymentMethod;
import com.yoen.yoen_back.enums.PaymentType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** 결제 기록 엔티티
 */
@Getter
@Setter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(name = "payments")
public class Payment extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;

    private String paymentName;

    private Double exchangeRate;

    @JoinColumn(name = "travel_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Travel travel;

    @JoinColumn(name = "category_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Category category;

    // 결제자
    @JoinColumn(name = "traveluser_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private TravelUser travelUser;

    // 카드 or 현금
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    // 개인 or 공금
    @Enumerated(EnumType.STRING)
    private Payer payerType;

    private LocalDateTime payTime;

    private Long paymentAccount;

    @Enumerated(EnumType.STRING)
    private Currency currency;

    @Enumerated(EnumType.STRING)
    private PaymentType type = PaymentType.PAYMENT;

}
