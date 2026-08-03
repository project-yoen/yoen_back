package com.yoen.yoen_back.entity;

import com.yoen.yoen_back.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(name = "exchangerates", indexes = {
        // 결제 시점 기준 최신 환율 조회 (created_at 내림차순 top-1)
        @Index(name = "idx_exchangerates_createdat", columnList = "created_at")
})
public class ExchangeRate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long exchangeRateId;

    @Column(nullable = false)
    private Double exchangeRate;

}
