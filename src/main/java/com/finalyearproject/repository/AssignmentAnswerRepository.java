package com.finalyearproject.repository;

import com.finalyearproject.model.AssignmentAnswer;
import com.finalyearproject.model.AssignmentQuestion;
import com.finalyearproject.model.Submission;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssignmentAnswerRepository extends JpaRepository<AssignmentAnswer, Long> {
    List<AssignmentAnswer> findBySubmission(Submission submission);
    List<AssignmentAnswer> findByQuestion(AssignmentQuestion question);
}
