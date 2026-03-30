package com.finalyearproject.service;

import com.finalyearproject.model.User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StudyGroupService {

    public List<String> getUserGroups(User user){
        return List.of();
    }

    public List<String> getRecommendedGroups(User user){
        return List.of();
    }

    public List<String> getUpcomingSessions(User user){
        return List.of();
    }
}