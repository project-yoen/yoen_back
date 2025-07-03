package com.yoen.yoen_back.entity.user;

import com.yoen.yoen_back.common.entity.BaseEntity;
import com.yoen.yoen_back.enums.Platform;
import jakarta.persistence.*;
import lombok.*;

/** 파이어베이스 토큰 엔티티
 */

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "firebasetokens")
public class FirebaseToken extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long firebaseTokenId;

    @JoinColumn(name = "user_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @Column(nullable = false)
    private String firebaseToken;

    private Platform platform;

    private String appVersion;
}
