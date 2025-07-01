package com.yoen.yoen_back.repository.user;

import com.yoen.yoen_back.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}