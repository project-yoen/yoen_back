package com.yoen.yoen_back.repository;

import com.yoen.yoen_back.entity.travel.Destination;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DestinationRepository extends JpaRepository<Destination, Long> {
}
