package com.yoen.yoen_back.repository.jpa.travel;

import com.yoen.yoen_back.entity.travel.TravelUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TravelUserRepository extends JpaRepository<TravelUser, Long> {
}
