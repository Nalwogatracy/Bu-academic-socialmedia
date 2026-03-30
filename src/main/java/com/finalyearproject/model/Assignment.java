package com.finalyearproject.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
public class Assignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @ManyToOne
    private User assignedTo;

    private String status; // "PENDING", "SUBMITTED", etc.

    private LocalDateTime dueDate;
    private String description;   // ← must exist
    private Integer totalPoints;
    private LocalDateTime createdAt; // ← must exist
    private String visibility;
  
    
    @ManyToOne
    private Course course;
    
     @ManyToOne
    private User lecturer;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(Integer totalPoints) {
        this.totalPoints = totalPoints;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public User getLecturer() {
        return lecturer;
    }

    public void setLecturer(User lecturer) {
        this.lecturer = lecturer;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public User getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(User assignedTo) {
        this.assignedTo = assignedTo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }
    @Transient
    private int totalStudents;

    public int getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(int totalStudents) {
        this.totalStudents = totalStudents;
    }
    @Transient
    public String getDaysLeftText() {
        if (dueDate == null) return "No deadline";

        long days = ChronoUnit.DAYS.between(LocalDateTime.now(), dueDate);

        if (days < 0) {
            return "Expired";
        } else if (days == 0) {
            return "Due today";
        } else if (days == 1) {
            return "1 day left";
        } else {
            return days + " days left";
        }
    }
    @Transient
    private int submitted;

    @Transient
    public int getSubmissionPercentage() {
        if (totalStudents == 0) return 0;

        return (int) ((submitted * 100.0) / totalStudents);
    }
    @Transient
    private int pendingGrading;

    public int getPendingGrading() {
        return pendingGrading;
    }

    public void setPendingGrading(int pendingGrading) {
        this.pendingGrading = pendingGrading;
    }
    public int getSubmitted() {
    return submitted;
    }

    public void setSubmitted(int submitted) {
        this.submitted = submitted;
    }
}