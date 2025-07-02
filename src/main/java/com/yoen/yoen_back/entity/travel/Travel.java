package com.yoen.yoen_back.entity.travel;

import com.yoen.yoen_back.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** 여행 엔티티
 * 여행의 정보를 관리
 */

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "travels")
public class Travel extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long travelId;

    @Column(nullable = false)
    private String travelName;

    @Column(nullable = false)
    private Long numOfPeople;

    private String nation;

//    @Column(nullable = false)
    private LocalDate startDate;

//    @Column(nullable = false)
    private LocalDate endDate;

    private Long sharedFund;

}
