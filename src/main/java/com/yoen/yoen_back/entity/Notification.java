package com.yoen.yoen_back.entity;

import com.yoen.yoen_back.common.entity.BaseEntity;
import com.yoen.yoen_back.entity.user.User;
import jakarta.persistence.*;
import lombok.*;

/** 알림 엔티티
 * 알림을 관리
 */

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "notifications")
public class Notification extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;

    @JoinColumn(name = "user_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    private String title;
    private String text;

    @Column(nullable = false)
    private String type;

    private String refId;

    private Boolean isRead = false;
}
