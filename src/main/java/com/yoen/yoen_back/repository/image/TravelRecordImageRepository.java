package com.yoen.yoen_back.repository.image;

import com.yoen.yoen_back.entity.image.TravelRecordImage;
import com.yoen.yoen_back.entity.travel.TravelRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TravelRecordImageRepository extends JpaRepository<TravelRecordImage, Long> {
    Optional<TravelRecordImage> findByTravelRecordImageIdAndIsActiveTrue(Long travelRecordImageId);

    List<TravelRecordImage> findAllByTravelRecord_TravelRecordId(Long travelRecordId);

    List<TravelRecordImage> findByTravelRecordAndIsActiveTrue(TravelRecord tr);
}
