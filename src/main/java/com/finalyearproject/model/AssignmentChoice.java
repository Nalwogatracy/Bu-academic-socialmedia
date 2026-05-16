package com.finalyearproject.model;

import jakarta.persistence.*;

@Entity
@Table(name = "assignment_choices")
public class AssignmentChoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private AssignmentQuestion question;

    @Column(nullable = false, length = 2000)
    private String text;

    private boolean correct;
    private int orderIndex;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public AssignmentQuestion getQuestion() { return question; }
    public void setQuestion(AssignmentQuestion question) { this.question = question; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public boolean isCorrect() { return correct; }
    public void setCorrect(boolean correct) { this.correct = correct; }

    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }
}
