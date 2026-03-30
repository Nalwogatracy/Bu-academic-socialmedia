package com.finalyearproject.service;

import com.finalyearproject.model.Deadline;
import com.finalyearproject.model.User;
import com.finalyearproject.repository.DeadlineRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DeadlineService {

    private final DeadlineRepository deadlineRepository;

    public DeadlineService(DeadlineRepository deadlineRepository) {
        this.deadlineRepository = deadlineRepository;
    }

    public List<Deadline> getUpcomingDeadlinesForUser(User user) {
        return deadlineRepository.findUpcomingForUser(user);
    }
}