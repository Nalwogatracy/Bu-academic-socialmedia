
package com.finalyearproject.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_status")
public class UserStatus {

    @Id
    private Long userId; // one-to-one with User

    private boolean online;
    private LocalDateTime lastSeen;

    @OneToOne
    @MapsId
    private User user;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public LocalDateTime getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(LocalDateTime lastSeen) {
        this.lastSeen = lastSeen;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
    
    @Transient
    public String getLastSeenDisplay() {

        if (online) {
            return "Online";
        }

        if (lastSeen == null) {
            return "Never";
        }

        Duration duration = Duration.between(lastSeen, LocalDateTime.now());

        long minutes = duration.toMinutes();
        long hours = duration.toHours();
        long days = duration.toDays();

        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + " min ago";
        if (hours < 24) return hours + " hrs ago";

        return days + " days ago";
    }
}
