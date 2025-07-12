package com.yoen.yoen_back.repository.travel;

import com.yoen.yoen_back.entity.travel.TravelUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TravelUserRepository extends JpaRepository<TravelUser, Long> {
    List<TravelUser> findByTravel_TravelId(Long travelId);
}
