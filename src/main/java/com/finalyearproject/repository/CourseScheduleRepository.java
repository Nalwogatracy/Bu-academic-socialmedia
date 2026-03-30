package com.finalyearproject.repository;

import com.finalyearproject.model.CourseSchedule;
import com.finalyearproject.model.Course;
import com.finalyearproject.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseScheduleRepository extends JpaRepository<CourseSchedule, Long> {

    // Find all schedules for a specific course
    List<CourseSchedule> findByCourse(Course course);

    // Find schedules by course and lecturer (optional)
    List<CourseSchedule> findByCourseAndLecturer(Course course, User lecturer);
}