package com.finalyearproject.service;

import com.finalyearproject.controller.SseController;
import com.finalyearproject.model.Message;
import com.finalyearproject.model.User;
import com.finalyearproject.repository.MessageRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    
    private final SseController sseController;

    public MessageService(MessageRepository messageRepository,@Lazy SseController sseController) {
        this.messageRepository = messageRepository;
        this.sseController = sseController;
    }

    // Count unread messages for a user
    public int countUnread(User user) {
        return messageRepository.countByRecipientAndReadFalse(user);
    }

    // Get all messages involving a user (as sender or recipient)
    public List<Message> getMessagesForUser(User user) {
        return messageRepository.findMessagesForUser(user);
    }

    // Get conversations grouped by the other participant
    public Map<User, List<Message>> getConversationsForUser(User user) {
        List<Message> messages = messageRepository.findMessagesForUser(user);
        Map<User, List<Message>> conversations = new HashMap<>();

        for (Message m : messages) {
            // Determine the "other participant" in the conversation
            User other = m.getSender().equals(user) ? m.getRecipient() : m.getSender();
            conversations.computeIfAbsent(other, k -> new ArrayList<>()).add(m);
        }

        return conversations;
    }

    // Send a message
    @Transactional
    public Message sendMessage(User sender, User recipient, String content, String subject) {
        Message message = new Message();
        message.setSender(sender);
        message.setRecipient(recipient);
        message.setContent(content);
        message.setSubject(subject);
        message.setSentAt(LocalDateTime.now());
        message.setRead(false);
        Message savedMessage = messageRepository.save(message);

        //return messageRepository.save(message);
        
         sseController.pushBadgeUpdate(recipient);

        // ── Push toast notification to recipient ──────────────────────────────
        sseController.pushNotification(recipient,
            "New Message",
            sender.getFullName() + " sent you a message");
        return savedMessage;

    }
    // In MessageService — add these methods
    public List<Message> getConversation(User user1, User user2) {
        return messageRepository.findBySenderAndRecipientOrSenderAndRecipientOrderBySentAtAsc(
            user1, user2, user2, user1);
    }

    public void markConversationAsRead(User reader, User other) {
        messageRepository.findBySenderAndRecipientOrSenderAndRecipientOrderBySentAtAsc(
            other, reader, reader, other)
            .stream()
            .filter(m -> m.getRecipient().getId().equals(reader.getId()) && !m.isRead())
            .forEach(m -> {
                m.setRead(true);
                messageRepository.save(m);
            });
    }

    
}