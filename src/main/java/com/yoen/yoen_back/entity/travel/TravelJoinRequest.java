package com.yoen.yoen_back.entity.travel;


import com.yoen.yoen_back.common.entity.BaseEntity;
import com.yoen.yoen_back.entity.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 여행 참여 신청 엔티티
 * 여행 가입 신청을 한 유저들을 관리하는 테이블
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "traveljoinrequests")
public class TravelJoinRequest extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long travelJoinRequestId;

    @JoinColumn(name = "travel_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Travel travel;

    @JoinColumn(name = "user_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    private Boolean isAccepted;
}
