package com.finalyearproject.service;

import com.finalyearproject.model.*;
import com.finalyearproject.repository.AssignmentAnswerRepository;
import com.finalyearproject.repository.AssignmentRepository;
import com.finalyearproject.repository.SubmissionRepository;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssignmentService {
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final AssignmentAnswerRepository assignmentAnswerRepository;
    private final CourseService courseService;

    public AssignmentService(AssignmentRepository assignmentRepository, SubmissionRepository submissionRepository,
                             AssignmentAnswerRepository assignmentAnswerRepository, CourseService courseService) {
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.assignmentAnswerRepository = assignmentAnswerRepository;
        this.courseService = courseService;
    }

    public int countPending(User user) {
        return assignmentRepository.countByAssignedToAndStatus(user, "PENDING");
    }
    public List<Assignment> getCourseAssignments(Long courseId) {
        return assignmentRepository.findByCourseId(courseId);
    }
    public int countPendingAssignments(Long courseId) {
        return assignmentRepository.findByCourseId(courseId).size();
    }
    
    public long countUngradedForCourses(List<Course> courses) {
        return submissionRepository.countByAssignmentCourseInAndGradedFalse(courses);
    }

    public long countGradedForCourse(Course course) {
        return submissionRepository.countByAssignmentCourseAndGradedTrue(course);
    }

    public long countUngradedForCourse(Course course) {
        return submissionRepository.countByAssignmentCourseAndGradedFalse(course);
    }

    // ── Recent submissions for the activity feed ──────────────────────────────
    public List<Map<String, Object>> getRecentSubmissionsForLecturer(User lecturer, int limit) {
        List<Course> courses = courseService.getCoursesForLecturer(lecturer);
        return submissionRepository
                .findByAssignmentCourseInOrderBySubmittedAtDesc(courses)
                .stream()
                .limit(limit)
                .map(sub -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("studentName",     sub.getStudent().getFullName());
                    m.put("assignmentTitle", sub.getAssignment().getTitle());
                    m.put("timeAgo",         timeAgo(sub.getSubmittedAt()));
                    return m;
                })
                .collect(Collectors.toList());
    }

    // ── Pending reviews list for the dashboard review cards ───────────────────
    public List<Map<String, Object>> getPendingReviewsForLecturer(User lecturer) {
        List<Course> courses = courseService.getCoursesForLecturer(lecturer);
        return submissionRepository
                .findByAssignmentCourseInAndGradedFalseOrderBySubmittedAtAsc(courses)
                .stream()
                .map(sub -> {
                    long days = Duration.between(sub.getSubmittedAt(), LocalDateTime.now()).toDays();
                    Map<String, Object> r = new LinkedHashMap<>();
                    r.put("submissionId",    sub.getId());
                    r.put("studentName",     sub.getStudent().getFullName());
                    r.put("studentInitials", initials(sub.getStudent().getFullName()));
                    r.put("assignmentTitle", sub.getAssignment().getTitle());
                    r.put("submittedAgo",    timeAgo(sub.getSubmittedAt()));
                    r.put("fileName",        sub.getFileName() != null ? sub.getFileName() : "submission.pdf");
                    r.put("fileIcon",        fileIcon(sub.getFileName()));
                    r.put("urgent",          days >= 2);
                    return r;
                })
                .collect(Collectors.toList());
    }

    // ── Grade a submission ────────────────────────────────────────────────────
    @Transactional
    public void gradeSubmission(Long submissionId, int score, String feedback, User gradedBy) {
        Submission sub = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found: " + submissionId));
        sub.setScore(score);
        sub.setFeedback(feedback);
        sub.setGraded(true);
        sub.setGradedAt(LocalDateTime.now());
        sub.setGradedBy(gradedBy);
        submissionRepository.save(sub);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String timeAgo(LocalDateTime dt) {
        if (dt == null) return "Unknown";
        long mins = Duration.between(dt, LocalDateTime.now()).toMinutes();
        if (mins < 1)    return "Just now";
        if (mins < 60)   return mins + " min ago";
        if (mins < 1440) return (mins / 60) + " hours ago";
        if (mins < 2880) return "Yesterday";
        return (mins / 1440) + " days ago";
    }

    private String initials(String fullName) {
        if (fullName == null || fullName.isBlank()) return "??";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    private String fileIcon(String fileName) {
        if (fileName == null) return "fas fa-file-alt";
        String ext = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase() : "";
        return switch (ext) {
            case "pdf"         -> "fas fa-file-pdf";
            case "ppt","pptx"  -> "fas fa-file-powerpoint";
            case "doc","docx"  -> "fas fa-file-word";
            case "zip","rar"   -> "fas fa-file-archive";
            case "jpg","png"   -> "fas fa-file-image";
            default            -> "fas fa-file-alt";
        };
    }
    // Count all ungraded submissions across many courses
    public long countPendingForCourses(List<Course> courses) {
        return submissionRepository.countByAssignmentCourseInAndGradedFalse(courses);
    }

    // Count pending submissions for ONE course
    public long countPendingForCourse(Course course) {
        return submissionRepository.countByAssignmentCourseAndGradedFalse(course);
    }
    // Count submitted (graded) assignments for a course
    public long countSubmittedForCourse(Course course) {
        return submissionRepository.countByAssignmentCourseAndGradedTrue(course);
    }

    public long countPendingGradesForLecturer(User lecturer) {

        List<Course> courses = courseService.getCoursesForLecturer(lecturer);

        return submissionRepository.countByAssignmentCourseInAndGradedFalse(courses);
    }
    public boolean hasPendingGrading(Assignment assignment) {
        return submissionRepository.countByAssignmentAndGradedFalse(assignment) > 0;
    }
    public Assignment saveAssignment(Assignment assignment) {
        return assignmentRepository.save(assignment);
    }
    public Assignment getAssignmentById(Long id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Assignment not found with id: " + id));
    }
    public long countByCourse(Course course) {
        return assignmentRepository.countByCourse(course);
    }
    public double getAverageScoreForCourse(Course course) {
        List<Submission> submissions = submissionRepository.findByAssignmentCourseIn(List.of(course));
        if (submissions.isEmpty()) return 0.0;
        double sum = submissions.stream()
                                .filter(Submission::getGraded)
                                .mapToDouble(Submission::getScore)
                                .sum();
        long count = submissions.stream().filter(Submission::getGraded).count();
        return count == 0 ? 0.0 : sum / count;
    }
    public double getSubmissionRateForCourse(Course course) {
        List<Assignment> assignments = assignmentRepository.findByCourse(course);
        if (assignments.isEmpty()) return 0.0;

        long totalSubmissions = assignments.stream()
            .flatMap(a -> submissionRepository.findByAssignment(a).stream())
            .count();

        long totalAssignments = assignments.size();
        return totalAssignments == 0 ? 0.0 : (double) totalSubmissions / totalAssignments;
    }
    // AssignmentService.java
    public long countByLecturer(User lecturer) {
        List<Course> courses = courseService.getCoursesForLecturer(lecturer);
        return assignmentRepository.countByCourseIn(courses);
    }
    public Map<LocalDate, Long> getSubmissionTrendsForCourse(Course course) {
        List<Submission> submissions = submissionRepository.findByAssignmentCourseIn(List.of(course));
        return submissions.stream()
                .collect(Collectors.groupingBy(
                    s -> s.getSubmittedAt().toLocalDate(),
                    TreeMap::new,
                    Collectors.counting()
                ));
    }
    public Map<String, Long> getGradeDistributionForCourse(Course course) {
        List<Submission> submissions = submissionRepository.findByAssignmentCourseIn(List.of(course));
        return submissions.stream()
                .collect(Collectors.groupingBy(
                    s -> {
                        int score = s.getScore() != null ? s.getScore() : 0;
                        if (score >= 90) return "A";
                        if (score >= 80) return "B";
                        if (score >= 70) return "C";
                        if (score >= 60) return "D";
                        return "F";
                    },
                    Collectors.counting()
                ));
    }

   /* public List<Submission> generateGradeReport(Course course) {
        // Get all assignments for the course
        List<Assignment> assignments = assignmentRepository.findByCourse(course);

        // Extract unique courses (optional here since it's one course, but safe if using multiple later)
        List<Course> courses = assignments.stream()
            .map(Assignment::getCourse)
            .distinct()
            .toList();

        // Fetch all submissions for these assignments' courses
        return submissionRepository.findByAssignmentCourseIn(courses);
    } */
     public List<Submission> getRecentSubmissionsForCourse(Course course, int limit) {
        List<Submission> submissions = submissionRepository.findByAssignmentCourseInOrderBySubmittedAtDesc(List.of(course));
        return submissions.stream().limit(limit).toList();
    }
     
      public byte[] generateGradeReport(Course course) {
        // Get all assignments for this course
        List<Assignment> assignments = assignmentRepository.findByCourse(course);

        // Get all submissions for these assignments
        List<Submission> submissions = submissionRepository.findByAssignmentInOrderBySubmittedAtDesc(assignments);

        // Build CSV
        StringBuilder csv = new StringBuilder();
        csv.append("Student,Assignment,Score\n");

        for (Submission sub : submissions) {
            csv.append(sub.getStudent().getFullName())
               .append(",")
               .append(sub.getAssignment().getTitle())
               .append(",")
               .append(sub.getScore() != null ? sub.getScore() : "N/A")
               .append("\n");
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }
      // In AssignmentService
    public boolean hasStudentSubmitted(Assignment assignment, User student) {
        return submissionRepository.existsByAssignmentAndStudent(assignment, student);
    }

    public boolean isGradedForStudent(Assignment assignment, User student) {
        return submissionRepository.findByAssignmentAndStudent(assignment, student)
            .map(Submission::getGraded)
            .orElse(false);
    }
    public List<Submission> getSubmissionsForStudent(Course course, User student) {
        return submissionRepository.findByAssignmentCourseIn(List.of(course))
            .stream()
            .filter(s -> s.getStudent().getId().equals(student.getId()))
            .collect(Collectors.toList());
    }

    public void saveSubmission(Submission submission) {
        submissionRepository.save(submission);
    }

    public Optional<Submission> getSubmissionForStudent(Assignment assignment, User student) {
        return submissionRepository.findByAssignmentAndStudent(assignment, student);
    }

    @Transactional
    public Submission autoGrade(Submission submission, Map<Long, Long> selectedChoices, Map<Long, String> textAnswers) {
        Assignment assignment = submission.getAssignment();
        int totalScore = 0;
        boolean hasShortAnswer = false;

        if ("QUIZ".equals(assignment.getSubmissionType()) || "MIXED".equals(assignment.getSubmissionType())) {
            for (AssignmentQuestion question : assignment.getQuestions()) {
                AssignmentAnswer answer = new AssignmentAnswer();
                answer.setSubmission(submission);
                answer.setQuestion(question);

                if ("SHORT_ANSWER".equals(question.getQuestionType())) {
                    String textAns = textAnswers.get(question.getId());
                    answer.setTextAnswer(textAns != null ? textAns : "");
                    answer.setCorrect(false);
                    answer.setPointsEarned(0);
                    hasShortAnswer = true;
                } else {
                    Long choiceId = selectedChoices.get(question.getId());
                    if (choiceId != null) {
                        for (AssignmentChoice choice : question.getChoices()) {
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
                submission.getAnswers().add(answer);
            }
        }

        if ("EXACT_MATCH".equals(assignment.getAutoGradeType()) && submission.getTextAnswer() != null
                && assignment.getAutoGradeAnswer() != null && !assignment.getAutoGradeAnswer().isBlank()) {
            if (submission.getTextAnswer().trim().equalsIgnoreCase(assignment.getAutoGradeAnswer().trim())) {
                totalScore += assignment.getAutoGradePoints() != null ? assignment.getAutoGradePoints() : 0;
            }
        } else if ("KEYWORD_MATCH".equals(assignment.getAutoGradeType()) && submission.getTextAnswer() != null
                && assignment.getAutoGradeAnswer() != null && !assignment.getAutoGradeAnswer().isBlank()) {
            String[] keywords = assignment.getAutoGradeAnswer().toLowerCase().split("\\s*,\\s*");
            String answer = submission.getTextAnswer().toLowerCase();
            boolean allFound = true;
            for (String kw : keywords) {
                if (!answer.contains(kw.trim())) {
                    allFound = false;
                    break;
                }
            }
            if (allFound) {
                totalScore += assignment.getAutoGradePoints() != null ? assignment.getAutoGradePoints() : 0;
            }
        }

        if (hasShortAnswer || "FILE_UPLOAD".equals(assignment.getSubmissionType())) {
            submission.setScore(totalScore);
            submission.setGraded(false);
        } else {
            submission.setScore(totalScore);
            submission.setGraded(true);
            submission.setGradedAt(LocalDateTime.now());
        }

        return submissionRepository.save(submission);
    }
}