package com.yoen.yoen_back.repository;

import com.yoen.yoen_back.entity.user.FirebaseToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FirebaseTokenRepository extends JpaRepository<FirebaseToken, Long> {
}
