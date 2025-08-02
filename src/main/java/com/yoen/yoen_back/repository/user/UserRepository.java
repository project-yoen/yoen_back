package com.yoen.yoen_back.repository.user;

import com.yoen.yoen_back.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailAndIsActiveTrue(String email);
    Optional<User> findByUserIdAndIsActiveTrue(Long userId);
    Boolean existsByEmailAndIsActiveTrue(String email);

}