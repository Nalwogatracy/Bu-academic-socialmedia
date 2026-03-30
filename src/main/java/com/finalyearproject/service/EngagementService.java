package com.finalyearproject.service;

import com.finalyearproject.model.User;
import org.springframework.stereotype.Service;

@Service
public class EngagementService {

    public int calculateEngagement(User user) {
        // Mock calculation; implement real logic later
        return 75; // returns 75% engagement
    }
}