package com.finalyearproject.repository;

import com.finalyearproject.model.Quiz;
import com.finalyearproject.model.QuizAttempt;
import com.finalyearproject.model.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    List<QuizAttempt> findByQuizAndStudent(Quiz quiz, User student);
    Optional<QuizAttempt> findById(Long id);
    List<QuizAttempt> findByQuizOrderBySubmittedAtDesc(Quiz quiz);
    long countByQuizAndStudent(Quiz quiz, User student);
    int countByQuizAndStudentAndStatus(Quiz quiz, User student, String status);
    Optional<QuizAttempt> findTopByQuizAndStudentAndStatusOrderByStartedAtDesc(Quiz quiz, User student, String status);
}
