package com.yoen.yoen_back.entity.travel;

import com.yoen.yoen_back.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** 여행 기록 엔티티
 *  여행 기록으 관리하는 테이블
 */

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "travelrecords")
public class TravelRecord extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long travelRecordId;

    @JoinColumn(name = "travel_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Travel travel;

    @JoinColumn(name = "traveluser_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private TravelUser travelUser;

    @Column(nullable = false)
    private String title;
    private String content;

    @Column(nullable = false)
    private LocalDateTime recordTime;

}
