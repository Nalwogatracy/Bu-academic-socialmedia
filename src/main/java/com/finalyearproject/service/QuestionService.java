package com.finalyearproject.service;

import com.finalyearproject.model.Course;
import com.finalyearproject.model.Question;
import com.finalyearproject.model.User;
import com.finalyearproject.repository.CourseRepository;
import com.finalyearproject.repository.QuestionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class QuestionService {

    private final QuestionRepository questionRepository;
    @Autowired
    private CourseRepository courseRepository;

    public QuestionService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    public List<Question> getUserQuestions(User user) {
        return questionRepository.findByAuthorOrderByCreatedAtDesc(user);
    }

    public List<Question> getPopularQuestions() {
        return questionRepository.findPopularQuestions();
    }
    public List<Question> getPinnedTopics(Long courseId) {
        if (courseId != null) {
            return questionRepository.findByPinnedTrueAndCourseId(courseId);
        }
        return questionRepository.findByPinnedTrue();
    }

    public List<Question> getQuestionsByCourse(Long courseId) {
        return questionRepository.findByCourseId(courseId);
    }

    public List<Question> getAllQuestions() {
        return questionRepository.findAll();
    }
    public List<Question> getAllQuestionsForUser(User user) {
        // Get courses the user is in
        List<Course> courses = courseRepository.findCoursesWithDetailsByStudent(user);
        // Return questions from those courses + public questions + user's own
        return questionRepository.findByCourseInOrAuthor(courses, user);
    }
    public Question saveQuestion(Question question) {
        return questionRepository.save(question);
    }
    
}