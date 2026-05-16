package com.finalyearproject.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CalendarEvent {

    public enum EventType {
        ASSIGNMENT, QUIZ, SCHEDULE, DEADLINE
    }

    private String title;
    private String description;
    private LocalDateTime dateTime;
    private LocalDateTime endDateTime;
    private EventType type;
    private String courseName;
    private String courseCode;
    private Long courseId;
    private String url;
    private String color;

    public CalendarEvent(String title, LocalDateTime dateTime, EventType type, String courseName, String courseCode, Long courseId, String url, String color) {
        this.title = title;
        this.dateTime = dateTime;
        this.type = type;
        this.courseName = courseName;
        this.courseCode = courseCode;
        this.courseId = courseId;
        this.url = url;
        this.color = color;
    }

    public CalendarEvent(String title, LocalDateTime dateTime, LocalDateTime endDateTime, EventType type, String courseName, String courseCode, Long courseId, String url, String color) {
        this.title = title;
        this.dateTime = dateTime;
        this.endDateTime = endDateTime;
        this.type = type;
        this.courseName = courseName;
        this.courseCode = courseCode;
        this.courseId = courseId;
        this.url = url;
        this.color = color;
    }

    public String getFormattedDate() {
        return dateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
    }

    public String getFormattedTime() {
        return dateTime.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    public String getFormattedEndTime() {
        return endDateTime != null ? endDateTime.format(DateTimeFormatter.ofPattern("HH:mm")) : null;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getDateTime() { return dateTime; }
    public LocalDateTime getEndDateTime() { return endDateTime; }
    public EventType getType() { return type; }
    public String getCourseName() { return courseName; }
    public String getCourseCode() { return courseCode; }
    public Long getCourseId() { return courseId; }
    public String getUrl() { return url; }
    public String getColor() { return color; }
    public String getTypeLabel() {
        return switch (type) {
            case ASSIGNMENT -> "Assignment";
            case QUIZ -> "Quiz";
            case SCHEDULE -> "Class";
            case DEADLINE -> "Deadline";
        };
    }
}
