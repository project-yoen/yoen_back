package com.yoen.yoen_back.repository.jpa.travel;

import com.yoen.yoen_back.entity.travel.TravelJoinRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TravelJoinRequestRepository extends JpaRepository<TravelJoinRequest, Long> {
}
