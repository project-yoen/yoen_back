package com.yoen.yoen_back.entity.image;

import com.yoen.yoen_back.common.entity.BaseEntity;
import com.yoen.yoen_back.entity.payment.PrePayment;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 사전사용금액_사진 엔티티
 */
@Getter
@Setter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "prepaymentimages")
public class PrePaymentImage extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long prePaymentImageId;

    @JoinColumn(name = "prepayment_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private PrePayment prepayment;

    @JoinColumn(name = "image_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Image image;
}
