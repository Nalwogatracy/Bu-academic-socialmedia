package com.finalyearproject.service;

import com.finalyearproject.model.Choice;
import com.finalyearproject.model.Course;
import com.finalyearproject.model.Quiz;
import com.finalyearproject.model.QuizQuestion;
import com.finalyearproject.model.User;
import com.finalyearproject.repository.QuizQuestionRepository;
import com.finalyearproject.repository.QuizRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuizService {
    private final QuizRepository quizRepository;
    private final QuizQuestionRepository quizQuestionRepository;

    public QuizService(QuizRepository quizRepository, QuizQuestionRepository quizQuestionRepository) {
        this.quizRepository = quizRepository;
        this.quizQuestionRepository = quizQuestionRepository;
    }

    @Transactional
    public Quiz createQuiz(Quiz quiz) {
        return quizRepository.save(quiz);
    }

    public Quiz getQuizById(Long id) {
        return quizRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quiz not found with id: " + id));
    }

    public List<Quiz> getQuizzesForCourse(Course course) {
        return quizRepository.findByCourseOrderByCreatedAtDesc(course);
    }

    public List<Quiz> getQuizzesForLecturer(User lecturer) {
        return quizRepository.findByCreatedBy(lecturer);
    }

    @Transactional
    public void addQuestion(Quiz quiz, QuizQuestion question) {
        question.setQuiz(quiz);
        if (question.getChoices() != null) {
            for (Choice choice : question.getChoices()) {
                choice.setQuestion(question);
            }
        }
        quizQuestionRepository.save(question);
        quiz.setTotalPoints(calculateTotalPoints(quiz));
        quizRepository.save(quiz);
    }

    public int calculateTotalPoints(Quiz quiz) {
        List<QuizQuestion> questions = quizQuestionRepository.findByQuizOrderByOrderIndexAsc(quiz);
        return questions.stream().mapToInt(QuizQuestion::getPoints).sum();
    }

    @Transactional
    public void deleteQuiz(Long id) {
        quizRepository.deleteById(id);
    }

    public long countByCourse(Course course) {
        return quizRepository.findByCourse(course).size();
    }
}
