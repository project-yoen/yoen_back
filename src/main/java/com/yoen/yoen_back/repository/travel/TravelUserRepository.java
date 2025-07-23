package com.yoen.yoen_back.repository.travel;

import com.yoen.yoen_back.entity.travel.Travel;
import com.yoen.yoen_back.entity.travel.TravelUser;
import com.yoen.yoen_back.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TravelUserRepository extends JpaRepository<TravelUser, Long> {
    List<TravelUser> findByTravel_TravelId(Long travelId);
    Optional<TravelUser> findByTravel_TravelIdAndUser(Long travelId, User user);

    List<TravelUser> findByTravelAndIsActiveTrue(Travel tv);
}
