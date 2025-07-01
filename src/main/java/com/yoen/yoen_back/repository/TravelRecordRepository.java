package com.yoen.yoen_back.repository;

import com.yoen.yoen_back.entity.travel.TravelRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TravelRecordRepository extends JpaRepository<TravelRecord, Long> {
}
