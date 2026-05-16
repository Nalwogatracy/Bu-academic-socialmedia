package com.finalyearproject.repository;

import com.finalyearproject.model.Course;
import com.finalyearproject.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {

    /**
     * Fetch all courses a student is enrolled in.
     * Course.students is Set<User> with mappedBy="courses" (inverse side).
     * MEMBER OF traverses the Set correctly — works for both List and Set.
     *
     * LEFT JOIN FETCH c.lecturer  → avoids N+1 on lecturer name
     * LEFT JOIN FETCH c.materials → avoids N+1 when counting materials
     *                               (materials is Set<Material>)
     */
    @Query("""
        SELECT DISTINCT c FROM Course c
        LEFT JOIN FETCH c.lecturer
        LEFT JOIN FETCH c.materials
        WHERE :student MEMBER OF c.students
        ORDER BY c.name ASC
    """)
    List<Course> findCoursesWithDetailsByStudent(@Param("student") User student);

    // Courses assigned to a lecturer (ManyToOne — simple equals)
    List<Course> findByLecturer(User lecturer);

    // Count courses for a student — used by CourseService.countCoursesForUser()
    @Query("SELECT COUNT(c) FROM Course c WHERE :student MEMBER OF c.students")
    int countByStudentsContains(@Param("student") User student);
    
    // In CourseRepository.java
    @Query("SELECT DISTINCT u FROM Course c JOIN c.students u WHERE c IN :courses AND u != :user")
    List<User> findCoursematesInCourses(@Param("courses") List<Course> courses, @Param("user") User user);
    
    @Query("SELECT c FROM Course c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(c.code) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(c.department) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<Course> search(@Param("q") String q);
}