package com.yoen.yoen_back.repository.travel;

import com.yoen.yoen_back.entity.travel.Travel;
import com.yoen.yoen_back.entity.travel.TravelUser;
import com.yoen.yoen_back.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TravelUserRepository extends JpaRepository<TravelUser, Long> {
    List<TravelUser> findByTravel_TravelId(Long travelId);
    Optional<TravelUser> findByTravel_TravelIdAndUserAndIsActiveTrue(Long travelId, User user);

    List<TravelUser> findByTravelAndIsActiveTrue(Travel tv);

    Optional<TravelUser> findByTravelUserIdAndIsActiveTrue(Long travelUserId);

    Optional<TravelUser> findByTravelAndUserAndIsActiveTrue(Travel travel, User user);

    @Query("SELECT tu.travel FROM TravelUser tu WHERE tu.user = :user AND tu.travel.isActive = true AND tu.isActive = true")
    List<Travel> findActiveTravelsByUser(@Param("user") User user);
}
