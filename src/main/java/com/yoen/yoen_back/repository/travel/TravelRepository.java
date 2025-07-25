package com.yoen.yoen_back.repository.travel;

import com.yoen.yoen_back.entity.travel.Travel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface TravelRepository extends JpaRepository<Travel, Long> {
    Optional<Travel> findByTravelIdAndIsActiveTrue(Long travelId);
}
