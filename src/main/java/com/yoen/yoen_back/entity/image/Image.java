package com.yoen.yoen_back.entity.image;

import com.yoen.yoen_back.common.entity.BaseEntity;
import com.yoen.yoen_back.entity.travel.Travel;
import jakarta.persistence.*;
import lombok.*;

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

    @JoinColumn(name = "travel_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Travel travel;
    
    @Column(nullable = false)
    private String imageUrl;
}
