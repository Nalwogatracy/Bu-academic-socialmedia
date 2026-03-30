package com.finalyearproject.repository;

import com.finalyearproject.model.Deadline;
import com.finalyearproject.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DeadlineRepository extends JpaRepository<Deadline, Long> {

    // Custom query to find upcoming deadlines for a user
    @Query("SELECT d FROM Deadline d WHERE d.course IN :#{#user.courses} AND d.dueDate >= CURRENT_DATE ORDER BY d.dueDate ASC")
    List<Deadline> findUpcomingForUser(User user);
}