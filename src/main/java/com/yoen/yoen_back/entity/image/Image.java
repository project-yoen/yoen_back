package com.yoen.yoen_back.entity.image;

import com.yoen.yoen_back.common.entity.BaseEntity;
import com.yoen.yoen_back.entity.travel.Travel;
import com.yoen.yoen_back.entity.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;

/**사진 엔티티
 * 사진 정보를 관리
 */
@Getter
@Setter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(name = "images")
public class Image extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long imageId;

    @JoinColumn(name = "user_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    User user;

    @Column(nullable = false)
    private String objectKey;

    @Column(nullable = false)
    private String imageUrl;
}

