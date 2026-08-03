package com.yoen.yoen_back.entity.travel;

import com.yoen.yoen_back.common.entity.BaseEntity;
import com.yoen.yoen_back.entity.user.User;
import com.yoen.yoen_back.enums.Role;
import jakarta.persistence.*;
import lombok.*;

/** 여행유저 엔티티
 * 여행에 속한 유저들의 정보를 관리, users와 travels의 중간 테이블 역할
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "travelusers", indexes = {
        // 여행별 참여자 조회 + 여행/유저 매칭 조회
        @Index(name = "idx_travelusers_travel_user", columnList = "travel_id, user_id"),
        // 유저의 여행 목록 조회
        @Index(name = "idx_travelusers_user", columnList = "user_id")
})
public class TravelUser extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long travelUserId;

    @JoinColumn(name = "travel_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Travel travel;

    @JoinColumn(name = "user_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @Enumerated(EnumType.STRING)
    private Role role;

    private String travelNickname;
}

