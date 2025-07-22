package com.yoen.yoen_back.repository.travel;

import com.yoen.yoen_back.entity.travel.TravelRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TravelRecordRepository extends JpaRepository<TravelRecord, Long> {
    List<TravelRecord> findByTravel_TravelId(Long travelId);
    List<TravelRecord> findByTravel_TravelIdAndIsActiveTrue(Long travelId);
    Optional<TravelRecord> findByTravelRecordIdAndIsActiveTrue(Long travelRecordId);
}
