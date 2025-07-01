package com.yoen.yoen_back.repository;

import com.yoen.yoen_back.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
