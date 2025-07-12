package com.yoen.yoen_back.repository.travel;

import com.yoen.yoen_back.entity.travel.TravelDestination;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TravelDestinationRepository extends JpaRepository<TravelDestination, Long> {
    List<TravelDestination> findByTravel_TravelId(Long travelId);
    List<TravelDestination> findByIsActiveTrue();
}
