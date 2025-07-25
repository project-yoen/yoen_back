package com.yoen.yoen_back.repository.travel;

import com.yoen.yoen_back.entity.travel.Travel;
import com.yoen.yoen_back.entity.travel.TravelJoinRequest;
import com.yoen.yoen_back.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TravelJoinRequestRepository extends JpaRepository<TravelJoinRequest, Long> {
    List<TravelJoinRequest> findByTravel_TravelIdAndIsActiveTrue(Long travelId);
    List<TravelJoinRequest> findByUserAndIsActiveTrueAndIsAcceptedFalse(User user);
    List<TravelJoinRequest> findByTravelAndUserAndIsActiveTrue(Travel tv, User user);

    Optional<TravelJoinRequest> findByTravelJoinRequestIdAndIsActiveTrue(Long travelJoinRequestId);
}
