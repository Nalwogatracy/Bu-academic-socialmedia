package com.finalyearproject.repository;

import com.finalyearproject.model.UserStatus;
import com.finalyearproject.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UserStatusRepository extends JpaRepository<UserStatus, Long> {

        // Get online peers for the user
        @Query("""
        SELECT DISTINCT us
        FROM UserStatus us
        JOIN us.user u
        JOIN u.courses c
        JOIN c.students s
        WHERE s = :user
          AND us.online = true
          AND us.user != :user
    """)
    List<UserStatus> findOnlinePeersForUser(User user);
    
    List<UserStatus> findByOnlineTrue();
}