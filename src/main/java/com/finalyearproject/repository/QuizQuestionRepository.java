package com.finalyearproject.repository;

import com.finalyearproject.model.Quiz;
import com.finalyearproject.model.QuizQuestion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {
    List<QuizQuestion> findByQuizOrderByOrderIndexAsc(Quiz quiz);
}
