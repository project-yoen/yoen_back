package com.yoen.yoen_back.repository;

import com.yoen.yoen_back.entity.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {
    Optional<ExchangeRate> findTopByCreatedAtLessThanOrderByCreatedAtDesc(LocalDateTime createdAt);

    Optional<ExchangeRate> findTopByOrderByCreatedAtAsc();
}
