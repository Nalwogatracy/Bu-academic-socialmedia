package com.finalyearproject.service;

import com.finalyearproject.model.User;
import com.finalyearproject.model.UserStatus;
import com.finalyearproject.repository.UserStatusRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserStatusService {

    private final UserStatusRepository userStatusRepository;

    public UserStatusService(UserStatusRepository userStatusRepository) {
        this.userStatusRepository = userStatusRepository;
    }

    public List<UserStatus> getOnlinePeersForUser(User user) {
        return userStatusRepository.findOnlinePeersForUser(user);
    }
    public List<UserStatus> getActiveStudyUsers() {
        return userStatusRepository.findByOnlineTrue();
    }
}