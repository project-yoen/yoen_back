package com.yoen.yoen_back.repository.image;

import com.yoen.yoen_back.entity.image.Image;
import com.yoen.yoen_back.entity.image.TravelRecordImage;
import com.yoen.yoen_back.entity.travel.TravelRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TravelRecordImageRepository extends JpaRepository<TravelRecordImage, Long> {
    Optional<TravelRecordImage> findByTravelRecordImageIdAndIsActiveTrue(Long travelRecordImageId);

    List<TravelRecordImage> findAllByTravelRecord_TravelRecordId(Long travelRecordId);

    List<TravelRecordImage> findByTravelRecordAndIsActiveTrue(TravelRecord tr);

    @Query("SELECT tri.image FROM TravelRecordImage tri WHERE tri.travelRecord.travel.travelId = :travelId AND tri.travelRecord.isActive = true ORDER BY tri.createdAt ASC")
    List<Image> findFirstByTravelOrderByCreatedAtAsc(Long travelId);

    @Query("""
SELECT tri
FROM TravelRecordImage tri
JOIN FETCH tri.travelRecord tr
JOIN FETCH tr.travel t
JOIN FETCH tri.image img
JOIN FETCH t.travelImage ti
WHERE tri.travelRecordImageId = :travelRecordImageId AND tri.isActive = true
""")
    Optional<TravelRecordImage> findWithTravelAndImageById(@Param("travelRecordImageId") Long id);
}
