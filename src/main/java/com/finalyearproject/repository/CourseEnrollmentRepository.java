package com.finalyearproject.repository;

import com.finalyearproject.model.CourseEnrollment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, Long> {

    @Query("SELECT e.course.id, COUNT(e) FROM CourseEnrollment e " +
           "WHERE e.course.id IN :courseIds AND e.roleInCourse = 'Student' " +
           "GROUP BY e.course.id")
    List<Object[]> countStudentsByCourses(@Param("courseIds") List<Long> courseIds);
}