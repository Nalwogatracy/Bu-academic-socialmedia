package com.finalyearproject.model;

import jakarta.persistence.*;

@Entity
@Table(name = "assignment_answers")
public class AssignmentAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    private Submission submission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private AssignmentQuestion question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_choice_id")
    private AssignmentChoice selectedChoice;

    @Column(length = 5000)
    private String textAnswer;

    private boolean correct;
    private int pointsEarned;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Submission getSubmission() { return submission; }
    public void setSubmission(Submission submission) { this.submission = submission; }

    public AssignmentQuestion getQuestion() { return question; }
    public void setQuestion(AssignmentQuestion question) { this.question = question; }

    public AssignmentChoice getSelectedChoice() { return selectedChoice; }
    public void setSelectedChoice(AssignmentChoice selectedChoice) { this.selectedChoice = selectedChoice; }

    public String getTextAnswer() { return textAnswer; }
    public void setTextAnswer(String textAnswer) { this.textAnswer = textAnswer; }

    public boolean isCorrect() { return correct; }
    public void setCorrect(boolean correct) { this.correct = correct; }

    public int getPointsEarned() { return pointsEarned; }
    public void setPointsEarned(int pointsEarned) { this.pointsEarned = pointsEarned; }
}
