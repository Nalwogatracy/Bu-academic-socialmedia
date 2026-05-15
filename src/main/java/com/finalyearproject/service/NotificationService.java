package com.finalyearproject.service;

import com.finalyearproject.controller.SseController;
import com.finalyearproject.model.Message;
import com.finalyearproject.model.Notification;
import com.finalyearproject.model.Post;
import com.finalyearproject.model.Submission;
import com.finalyearproject.model.User;
import com.finalyearproject.repository.MessageRepository;
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
    MessageRepository messageRepository;

    public NotificationService(NotificationRepository notificationRepository,@Lazy SseController sseController) {
        this.notificationRepository = notificationRepository;
        this.sseController = sseController;
    }

    public int countUnread(User user) {
        return notificationRepository.countByUserAndReadFalse(user);
    }
    public void sendGradeNotification(Submission submission) {

        User recipient = submission.getStudent(); // ✅ student receives
        User sender = submission.getAssignment().getLecturer(); // ✅ adjust if needed

        String message = "Your assignment '" +
                submission.getAssignment().getTitle() +
                "' has been graded.";

        Notification notification = new Notification();
        notification.setUser(recipient); // ⚠️ only ONE user field needed
        notification.setRecipient(recipient);
        notification.setSender(sender);
        notification.setMessage(message);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setRead(false);

        notificationRepository.save(notification);

        sseController.pushBadgeUpdate(recipient);
        sseController.pushNotification(
                recipient,
                sender != null ? sender.getFullName() : "System",
                message, sender
        );
    }
    public void sendAnnouncementEmail(Post post, Set<User> recipients) {
        for (User user : recipients) {
            // Build and send email or notification
            Notification notification = new Notification();
            notification.setUser(user);
            notification.setMessage("New announcement: " + post.getTitle());
            notification.setCreatedAt(LocalDateTime.now());
            notification.setRead(false);
            notificationRepository.save(notification);
        }
    }
    // Mark all messages as read for a user
    @Transactional
    public void markAllAsRead(User user) {
        List<Message> messages = messageRepository.findByRecipientOrderBySentAtDesc(user);
        messages.forEach(m -> m.setRead(true));
        messageRepository.saveAll(messages);
    }
    public List<Notification> getNotificationsForUser(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user); // ✅ correct
    }
}