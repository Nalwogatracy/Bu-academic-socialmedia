package com.finalyearproject.repository;

import com.finalyearproject.model.Question;
import com.finalyearproject.model.User;
import com.finalyearproject.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    // Get questions authored by a specific user
    List<Question> findByAuthorOrderByCreatedAtDesc(User author);

    // Popular questions this week (based on replies count)
    @Query("SELECT q FROM Question q ORDER BY SIZE(q.replies) DESC")
    List<Question> findPopularQuestions();

    // Questions by course
    List<Question> findByCourse(Course course);
    List<Question> findByPinnedTrue();
    List<Question> findByPinnedTrueAndCourseId(Long courseId);
    List<Question> findByCourseId(Long courseId);
    List<Question> findByCourseInOrAuthor(List<Course> courses, User author);
}