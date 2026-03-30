package com.finalyearproject.repository;

import com.finalyearproject.model.Submission;
import com.finalyearproject.model.Assignment;
import com.finalyearproject.model.Course;
import com.finalyearproject.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    List<Submission> findByAssignment(Assignment assignment);

    List<Submission> findByStudent(User student);

    //Submission findByAssignmentAndStudent(Assignment assignment, User student);
     // Count all ungraded submissions across many courses
    long countByAssignmentCourseInAndGradedFalse(List<Course> courses);

    // Count graded submissions for one course
    long countByAssignmentCourseAndGradedTrue(Course course);

    // Count ungraded submissions for one course
    long countByAssignmentCourseAndGradedFalse(Course course);
    
    // Find pending submissions ordered by submission time (oldest first)
    List<Submission> findByAssignmentCourseInAndGradedFalseOrderBySubmittedAtAsc(List<Course> courses);

    // Find recent submissions (latest first)
    List<Submission> findByAssignmentCourseInOrderBySubmittedAtDesc(List<Course> courses);
    long countByAssignmentAndGradedFalse(Assignment assignment);
    List<Submission> findByAssignmentOrderBySubmittedAtDesc(Assignment assignment);
    List<Submission> findByAssignmentCourseIn(List<Course> courses);
    List<Submission> findByAssignmentInOrderBySubmittedAtDesc(List<Assignment> assignments);
    boolean existsByAssignmentAndStudent(Assignment assignment, User student);
    Optional<Submission> findByAssignmentAndStudent(Assignment assignment, User student);
    

}