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

    List<TravelRecordImage> findAllByTravelRecordAndIsActiveTrue(TravelRecord travelRecord);

    List<TravelRecordImage> findByTravelRecordAndIsActiveTrue(TravelRecord tr);

    // 날짜별 기록 목록: 기록별 개별 조회(N+1) 대신 기록 리스트 전체의 이미지를 한 번에 fetch
    @Query("""
    SELECT tri
    FROM TravelRecordImage tri
    JOIN FETCH tri.image
    WHERE tri.travelRecord IN :travelRecords
      AND tri.isActive = true
    """)
    List<TravelRecordImage> findAllWithImageByTravelRecordIn(@Param("travelRecords") List<TravelRecord> travelRecords);

    @Query("SELECT tri.image FROM TravelRecordImage tri WHERE tri.travelRecord.travel.travelId = :travelId AND tri.travelRecord.isActive = true ORDER BY tri.createdAt ASC")
    List<Image> findFirstByTravelOrderByCreatedAtAsc(Long travelId);

    @Query("""
SELECT tri
FROM TravelRecordImage tri
JOIN FETCH tri.travelRecord tr
JOIN FETCH tr.travel t
JOIN FETCH tri.image img
LEFT JOIN FETCH t.travelImage ti
WHERE tri.travelRecordImageId = :travelRecordImageId AND tri.isActive = true
""")
    Optional<TravelRecordImage> findWithTravelAndImageById(@Param("travelRecordImageId") Long id);
}
