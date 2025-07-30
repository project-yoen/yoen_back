package com.yoen.yoen_back.repository.travel;

import com.yoen.yoen_back.entity.travel.Destination;
import com.yoen.yoen_back.enums.Nation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DestinationRepository extends JpaRepository<Destination, Long> {
    Optional<Destination> findByDestinationIdAndIsActiveTrue(Long destinationId);
    List<Destination> findByNationAndIsActiveTrue(Nation nation);
}
