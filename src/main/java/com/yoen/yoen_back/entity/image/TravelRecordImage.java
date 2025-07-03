package com.yoen.yoen_back.entity.image;

import com.yoen.yoen_back.common.entity.BaseEntity;
import com.yoen.yoen_back.entity.travel.TravelRecord;
import jakarta.persistence.*;
import lombok.*;

/** 여행기록_사진 엔티티
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "travelrecordimages")
public class TravelRecordImage extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long travelRecordImageId;

    @JoinColumn(name = "travelrecord_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private TravelRecord travelrecord;

    @JoinColumn(name = "image_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Image image;
}
