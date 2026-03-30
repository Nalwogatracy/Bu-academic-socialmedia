package com.finalyearproject.repository;

import com.finalyearproject.model.Notification;
import com.finalyearproject.model.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    int countByUserAndReadFalse(User user);
    List<Notification> findByUserOrderByCreatedAtDesc(User user);
}