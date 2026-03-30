package com.finalyearproject.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 2000)
    private String content;

    @ManyToOne
    private User author;

    @ManyToOne
    private Course course;

    private LocalDateTime createdAt;

    private boolean answered; // true if any reply exists
    private boolean pinned;
    
    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public int getViewCount() {
        return viewCount;
    }

    public void setViewCount(int viewCount) {
        this.viewCount = viewCount;
    }

    private int viewCount;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Message> replies; // optional if you want messages as replies

    public Question() {
        this.createdAt = LocalDateTime.now();
        this.answered = false;
    }
     @Transient
    public int getReplyCount() {
        return replies != null ? replies.size() : 0;
    }

    @Transient
    public String getExcerpt() {
        return content != null && content.length() > 100 ? content.substring(0, 100) + "..." : content;
    }

    @Transient
    public boolean isPopular() {
        return getReplyCount() > 10; // or any criteria
    }

    // --- Getters & Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public User getAuthor() { return author; }
    public void setAuthor(User author) { this.author = author; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isAnswered() { return answered; }
    public void setAnswered(boolean answered) { this.answered = answered; }

    public List<Message> getReplies() { return replies; }
    public void setReplies(List<Message> replies) { 
        this.replies = replies; 
        this.answered = replies != null && !replies.isEmpty();
    }
   
    //@Transient
    private boolean privateGroup = false;

    @ManyToMany
    @JoinTable(
        name = "question_group_members",
        joinColumns = @JoinColumn(name = "question_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> groupMembers = new ArrayList<>();

    public boolean isPrivateGroup() { return privateGroup; }
    public void setPrivateGroup(boolean privateGroup) { this.privateGroup = privateGroup; }
    public List<User> getGroupMembers() { return groupMembers; }
    public void setGroupMembers(List<User> groupMembers) { this.groupMembers = groupMembers; }
}