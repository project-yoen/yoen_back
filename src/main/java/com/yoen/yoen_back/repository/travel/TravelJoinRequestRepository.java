package com.yoen.yoen_back.repository.travel;

import com.yoen.yoen_back.entity.travel.TravelJoinRequest;
import com.yoen.yoen_back.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TravelJoinRequestRepository extends JpaRepository<TravelJoinRequest, Long> {
    List<TravelJoinRequest> findByUserAndIsActiveTrueAndIsAcceptedFalse(User user);
}
