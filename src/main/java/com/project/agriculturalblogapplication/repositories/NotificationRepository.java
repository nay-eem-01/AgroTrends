package com.project.agriculturalblogapplication.repositories;

import com.project.agriculturalblogapplication.entities.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
