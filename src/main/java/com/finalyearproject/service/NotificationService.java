package com.finalyearproject.service;

import com.finalyearproject.config.EmailService;
import com.finalyearproject.controller.SseController;
import com.finalyearproject.model.Notification;
import com.finalyearproject.model.Post;
import com.finalyearproject.model.Submission;
import com.finalyearproject.model.User;
import com.finalyearproject.repository.NotificationRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final SseController sseController;
    @Autowired
    private EmailService emailService;

    public NotificationService(NotificationRepository notificationRepository, @Lazy SseController sseController) {
        this.notificationRepository = notificationRepository;
        this.sseController = sseController;
    }

    public int countUnread(User user) {
        return notificationRepository.countByUserAndReadFalse(user);
    }

    public void sendGradeNotification(Submission submission) {
        User recipient = submission.getStudent();
        User sender = submission.getAssignment().getLecturer();

        String message = "Your assignment '" +
                submission.getAssignment().getTitle() +
                "' has been graded.";

        String scoreInfo = "Score: " + submission.getScore() +
                "/" + (submission.getAssignment().getTotalPoints() != null ?
                        submission.getAssignment().getTotalPoints() : "N/A");
        if (submission.getFeedback() != null && !submission.getFeedback().isBlank()) {
            scoreInfo += "\nFeedback: " + submission.getFeedback();
        }

        Notification notification = new Notification();
        notification.setUser(recipient);
        notification.setRecipient(recipient);
        notification.setSender(sender);
        notification.setMessage(message + " " + scoreInfo);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setRead(false);
        notificationRepository.save(notification);

        emailService.sendEmail(recipient.getEmail(),
                "Assignment Graded: " + submission.getAssignment().getTitle(),
                "Hello " + recipient.getFullName() + ",\n\n" +
                        message + "\n\n" + scoreInfo +
                        "\n\nLogin to view details: http://localhost:8080/student/assignment/" +
                        submission.getAssignment().getId() + "/view");

        sseController.pushBadgeUpdate(recipient);
        sseController.pushNotification(recipient,
                sender != null ? sender.getFullName() : "System",
                message, sender);
    }

    public void sendAnnouncementEmail(Post post, Set<User> recipients) {
        for (User user : recipients) {
            Notification notification = new Notification();
            notification.setUser(user);
            notification.setMessage("New announcement: " + post.getTitle());
            notification.setCreatedAt(LocalDateTime.now());
            notification.setRead(false);
            notification.setSender(post.getAuthor());
            notificationRepository.save(notification);

            emailService.sendEmail(user.getEmail(),
                    "New Announcement: " + post.getTitle(),
                    "Hello " + user.getFullName() + ",\n\n" +
                            post.getContent() +
                            "\n\n- " + post.getAuthor().getFullName() +
                            "\n\nLogin to view: http://localhost:8080/student/dashboard");

            sseController.pushBadgeUpdate(user);
        }
    }

    @Transactional
    public void markAllAsRead(User user) {
        List<Notification> notifications = notificationRepository.findByUserAndReadFalse(user);
        notifications.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(notifications);
    }

    public List<Notification> getNotificationsForUser(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }
}