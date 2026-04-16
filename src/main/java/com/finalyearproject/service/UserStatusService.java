package com.finalyearproject.service;

import com.finalyearproject.model.User;
import com.finalyearproject.model.UserStatus;
import com.finalyearproject.repository.UserStatusRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

import java.util.List;
import org.springframework.transaction.annotation.Transactional;

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
    public boolean isOnline(User user) {
        return userStatusRepository.findByUser(user)
            .map(UserStatus::isOnline)
            .orElse(false);
    }
    
    @Transactional
    public void markOnline(User user) {
        UserStatus status = userStatusRepository.findByUser(user)
            .orElseGet(() -> {
                UserStatus s = new UserStatus();
                s.setUser(user);
                return s;
            });
        status.setOnline(true);
        status.setLastSeen(LocalDateTime.now());
        userStatusRepository.save(status);
    }
    
    @Transactional
    public void markOffline(User user) {
        userStatusRepository.findByUser(user).ifPresent(status -> {
            status.setOnline(false);
            status.setLastSeen(LocalDateTime.now());
            userStatusRepository.save(status);
        });
    }
    
    @Transactional
    public void markInactiveUsersOffline() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(5);
        userStatusRepository.findByOnlineTrue().forEach(status -> {
            if (status.getLastSeen() != null && status.getLastSeen().isBefore(cutoff)) {
                status.setOnline(false);
                userStatusRepository.save(status);
            }
        });
    }
}