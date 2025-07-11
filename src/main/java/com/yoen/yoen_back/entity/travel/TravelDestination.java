package com.yoen.yoen_back.entity.travel;

import com.yoen.yoen_back.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/** 여행_목적지 엔티티
 * travels 테이블에 destinations 테이블을 연결하는 중간 테이블 역할
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "traveldestinations")
public class TravelDestination extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long travelDestinationId;

    @JoinColumn(name = "travel_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Travel travel;

    @JoinColumn(name = "destination_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Destination destination;

}
