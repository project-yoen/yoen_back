package com.yoen.yoen_back.entity;

import com.yoen.yoen_back.common.entity.BaseEntity;
import com.yoen.yoen_back.enums.Nation;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 목적지 엔티티
 * 나라별 지역 테이블
 * travelDestinations(여행목적지) 테이블에 조인되어 여행에 매핑
 */

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "destinations")
public class Destination extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long destinationId;

    @Enumerated(EnumType.STRING)
    private Nation nation;

    @Column(nullable = false)
    private String name;
}