package com.yoen.yoen_back.entity.travel;

import com.yoen.yoen_back.common.entity.BaseEntity;
import com.yoen.yoen_back.entity.image.Image;
import com.yoen.yoen_back.enums.Nation;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/** 여행 엔티티
 * 여행의 정보를 관리
 */

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
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

    @Column(nullable = false)
    private Long numOfJoinedPeople;

    @Enumerated(EnumType.STRING)
    private Nation nation;

//    @Column(nullable = false)
    private LocalDate startDate;

//    @Column(nullable = false)
    private LocalDate endDate;

    private Long sharedFund;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Image travelImage;

}
