package com.finalyearproject.repository;

import com.finalyearproject.model.Attendance;
import com.finalyearproject.model.Course;
import com.finalyearproject.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findByCourseAndDate(Course course, LocalDate date);
    List<Attendance> findByStudentAndCourse(User student, Course course);
    List<Attendance> findByCourse(Course course);
    long countByStudentAndCourseAndStatus(User student, Course course, String status);
    long countByCourseAndDate(Course course, LocalDate date);

    @Query("SELECT a FROM Attendance a WHERE a.course = :course AND a.date = :date AND a.student.id IN :studentIds")
    List<Attendance> findByCourseAndDateAndStudentIds(@Param("course") Course course, @Param("date") LocalDate date, @Param("studentIds") List<Long> studentIds);

    @Query("SELECT a.status, COUNT(a) FROM Attendance a WHERE a.course = :course AND a.date = :date GROUP BY a.status")
    List<Object[]> getStatusCountsForCourseAndDate(@Param("course") Course course, @Param("date") LocalDate date);
}
