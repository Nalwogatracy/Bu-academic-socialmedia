package com.finalyearproject.repository;

import com.finalyearproject.model.Assignment;
import com.finalyearproject.model.Course;
import com.finalyearproject.model.Submission;
import com.finalyearproject.model.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    int countByAssignedToAndStatus(User assignedTo, String status);
    List<Assignment> findByCourseId(Long courseId);
    long countByCourse(Course course);
    List<Assignment> findByCourse(Course course);
    long countByCourseIn(List<Course> courses);
    @Query("SELECT e.course.id, COUNT(e) FROM CourseEnrollment e " +
       "WHERE e.course.id IN :courseIds AND e.roleInCourse = 'Student' " +
       "GROUP BY e.course.id")
        List<Object[]> countStudentsByCourses(@Param("courseIds") List<Long> courseIds);
    
    

}