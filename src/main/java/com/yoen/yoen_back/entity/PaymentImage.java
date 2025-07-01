package com.yoen.yoen_back.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "paymentimages")
public class PaymentImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentImageId;

    @JoinColumn(name = "payment_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Payment payment;

    @JoinColumn(name = "image_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Image image;

}
