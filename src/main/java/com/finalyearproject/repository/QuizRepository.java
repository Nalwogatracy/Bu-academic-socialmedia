package com.finalyearproject.repository;

import com.finalyearproject.model.Course;
import com.finalyearproject.model.Quiz;
import com.finalyearproject.model.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {
    List<Quiz> findByCourse(Course course);
    List<Quiz> findByCourseOrderByCreatedAtDesc(Course course);
    List<Quiz> findByCreatedBy(User lecturer);
}
