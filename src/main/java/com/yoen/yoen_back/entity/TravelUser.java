package com.yoen.yoen_back.entity;

import com.yoen.yoen_back.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "travelusers")
public class TravelUser extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long travelUserId;

    @JoinColumn(name = "travels_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Travel travelId;

    @JoinColumn(name = "users_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private User userId;

    @JoinColumn(name = "roles_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Role roleId;

    private String travelNickname;
}
