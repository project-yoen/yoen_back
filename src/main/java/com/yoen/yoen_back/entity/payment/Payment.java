package com.yoen.yoen_back.entity.payment;

import com.yoen.yoen_back.common.entity.BaseEntity;
import com.yoen.yoen_back.entity.Category;
import com.yoen.yoen_back.entity.travel.Travel;
import com.yoen.yoen_back.enums.Payer;
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

    @JoinColumn(name = "travel_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Travel travel;

    @JoinColumn(name = "category_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Category category;

    @Enumerated(EnumType.STRING)
    private Payer payerType;

    private LocalDateTime payTime;

    private Long paymentAccount;

}
