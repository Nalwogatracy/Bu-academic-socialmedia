package com.finalyearproject.repository;

import com.finalyearproject.model.Message;
import com.finalyearproject.model.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    int countByRecipientAndReadFalse(User recipient);
    @Query("SELECT DISTINCT m.conversationId FROM Message m WHERE m.sender = :user OR m.recipient = :user ORDER BY m.sentAt DESC")
    List<Long> findConversationsByParticipant(@Param("user") User user);
    
    List<Message> findByRecipientOrderBySentAtDesc(User recipient);

    // Get all messages sent by a user
    List<Message> findBySenderOrderBySentAtDesc(User sender);

    // Get all messages involving a user (as sender or recipient)
    @Query("SELECT m FROM Message m WHERE m.sender = :user OR m.recipient = :user ORDER BY m.sentAt DESC")
    List<Message> findMessagesForUser(@Param("user") User user);

    // Count unread messages for a user
    @Query("SELECT COUNT(m) FROM Message m WHERE m.recipient = :user AND m.read = false")
    int countUnread(@Param("user") User user);

    List<Message> findBySenderAndRecipientOrSenderAndRecipientOrderBySentAtAsc(
    User sender1, User recipient1, User sender2, User recipient2);
    
    @Query("""
        SELECT COUNT(m) 
        FROM Message m 
        WHERE m.recipient = :user 
        AND m.read = false 
        AND m.conversationId = :conversationId
    """)
    int countUnreadByConversation(
            @Param("user") User user,
            @Param("conversationId") Long conversationId
    );
}