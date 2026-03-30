package com.finalyearproject.service;

import com.finalyearproject.model.User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DiscussionService {

    public List<String> getPinnedTopics(User user){
        return List.of();
    }

    public List<String> getTopicsForUser(User user){
        return List.of();
    }

    public List<String> getUserTopics(User user){
        return List.of();
    }

    public List<String> getPopularTopics(){
        return List.of();
    }

    public int countUnread(User user){
        return 0;
    }
}