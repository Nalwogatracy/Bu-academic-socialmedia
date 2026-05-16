package com.finalyearproject.service;

import com.finalyearproject.model.Choice;
import com.finalyearproject.model.Quiz;
import com.finalyearproject.model.QuizAnswer;
import com.finalyearproject.model.QuizAttempt;
import com.finalyearproject.model.QuizQuestion;
import com.finalyearproject.model.User;
import com.finalyearproject.repository.QuizAttemptRepository;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuizAttemptService {
    private final QuizAttemptRepository quizAttemptRepository;

    public QuizAttemptService(QuizAttemptRepository quizAttemptRepository) {
        this.quizAttemptRepository = quizAttemptRepository;
    }

    @Transactional
    public QuizAttempt startAttempt(Quiz quiz, User student) {
        QuizAttempt attempt = new QuizAttempt();
        attempt.setQuiz(quiz);
        attempt.setStudent(student);
        attempt.setStartedAt(LocalDateTime.now());
        attempt.setStatus("IN_PROGRESS");
        attempt.setTotalPoints(quiz.getTotalPoints());
        return quizAttemptRepository.save(attempt);
    }

    @Transactional
    public QuizAttempt submitAttempt(Long attemptId, Map<Long, Long> selectedChoices, Map<Long, String> textAnswers) {
        QuizAttempt attempt = quizAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found: " + attemptId));

        int totalScore = 0;

        for (QuizQuestion question : attempt.getQuiz().getQuestions()) {
            QuizAnswer answer = new QuizAnswer();
            answer.setAttempt(attempt);
            answer.setQuestion(question);

            if ("SHORT_ANSWER".equals(question.getQuestionType())) {
                String textAnswer = textAnswers.get(question.getId());
                answer.setTextAnswer(textAnswer != null ? textAnswer : "");
                answer.setCorrect(false);
                answer.setPointsEarned(0);
            } else {
                Long choiceId = selectedChoices.get(question.getId());
                if (choiceId != null) {
                    for (Choice choice : question.getChoices()) {
                        if (choice.getId().equals(choiceId)) {
                            answer.setSelectedChoice(choice);
                            answer.setCorrect(choice.isCorrect());
                            answer.setPointsEarned(choice.isCorrect() ? question.getPoints() : 0);
                            if (choice.isCorrect()) {
                                totalScore += question.getPoints();
                            }
                            break;
                        }
                    }
                } else {
                    answer.setCorrect(false);
                    answer.setPointsEarned(0);
                }
            }

            attempt.getAnswers().add(answer);
        }

        attempt.setScore(totalScore);
        attempt.setSubmittedAt(LocalDateTime.now());

        boolean hasShortAnswer = attempt.getQuiz().getQuestions().stream()
                .anyMatch(q -> "SHORT_ANSWER".equals(q.getQuestionType()));
        attempt.setStatus(hasShortAnswer ? "SUBMITTED" : "GRADED");

        return quizAttemptRepository.save(attempt);
    }

    public List<QuizAttempt> getAttemptsForQuiz(Quiz quiz) {
        return quizAttemptRepository.findByQuizOrderBySubmittedAtDesc(quiz);
    }

    public List<QuizAttempt> getAttemptsForStudent(Quiz quiz, User student) {
        return quizAttemptRepository.findByQuizAndStudent(quiz, student);
    }

    public QuizAttempt getAttemptById(Long id) {
        return quizAttemptRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Attempt not found: " + id));
    }

    public int getAttemptCount(Quiz quiz, User student) {
        return (int) quizAttemptRepository.countByQuizAndStudent(quiz, student);
    }

    public QuizAttempt getInProgressAttempt(Quiz quiz, User student) {
        return quizAttemptRepository.findTopByQuizAndStudentAndStatusOrderByStartedAtDesc(quiz, student, "IN_PROGRESS")
                .orElse(null);
    }

    public boolean isTimeExpired(QuizAttempt attempt) {
        Quiz quiz = attempt.getQuiz();
        if (quiz.getTimeLimitMinutes() == null || attempt.getStartedAt() == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(attempt.getStartedAt().plusMinutes(quiz.getTimeLimitMinutes()));
    }

    public Map<String, Object> getQuizStatistics(Quiz quiz) {
        Map<String, Object> stats = new HashMap<>();
        List<QuizAttempt> attempts = quizAttemptRepository.findByQuizOrderBySubmittedAtDesc(quiz);
        List<QuizAttempt> gradedAttempts = attempts.stream()
                .filter(a -> "GRADED".equals(a.getStatus()) || "SUBMITTED".equals(a.getStatus()))
                .toList();

        stats.put("totalAttempts", attempts.size());
        stats.put("gradedAttempts", gradedAttempts.size());

        int[] distribution = new int[4];
        int totalScoreSum = 0;
        int maxScore = 0;

        for (QuizAttempt a : gradedAttempts) {
            int pct = a.getTotalPoints() > 0 ? (a.getScore() * 100 / a.getTotalPoints()) : 0;
            totalScoreSum += pct;
            if (pct > maxScore) maxScore = pct;
            if (pct < 25) distribution[0]++;
            else if (pct < 50) distribution[1]++;
            else if (pct < 75) distribution[2]++;
            else distribution[3]++;
        }

        stats.put("dist_0_25", distribution[0]);
        stats.put("dist_25_50", distribution[1]);
        stats.put("dist_50_75", distribution[2]);
        stats.put("dist_75_100", distribution[3]);
        stats.put("avgScore", gradedAttempts.isEmpty() ? 0 : totalScoreSum / gradedAttempts.size());
        stats.put("maxScore", maxScore);

        List<Map<String, Object>> questionStats = new ArrayList<>();
        for (QuizQuestion q : quiz.getQuestions()) {
            Map<String, Object> qs = new HashMap<>();
            qs.put("id", q.getId());
            qs.put("text", q.getQuestionText());
            qs.put("type", q.getQuestionType());
            qs.put("points", q.getPoints());

            int correct = 0;
            int total = 0;
            for (QuizAttempt a : gradedAttempts) {
                for (QuizAnswer ans : a.getAnswers()) {
                    if (ans.getQuestion().getId().equals(q.getId())) {
                        total++;
                        if (ans.isCorrect()) correct++;
                    }
                }
            }
            qs.put("correctCount", correct);
            qs.put("totalCount", total);
            qs.put("correctPct", total > 0 ? (correct * 100 / total) : 0);
            questionStats.add(qs);
        }
        stats.put("questionStats", questionStats);

        return stats;
    }
}
