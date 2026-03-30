package com.finalyearproject.service;

import com.finalyearproject.model.Course;
import com.finalyearproject.model.CourseSchedule;
import com.finalyearproject.model.User;
import com.finalyearproject.repository.CourseRepository;
import com.finalyearproject.repository.CourseScheduleRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    @Autowired CourseScheduleRepository scheduleRepository;

    public CourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    public int countCoursesForUser(User user) {
        return courseRepository.countByStudentsContains(user);
    }

    @Transactional
    public List<Course> getCoursesForUser(User user) {
        return courseRepository.findCoursesWithDetailsByStudent(user);
    }
    
    @Transactional(readOnly = true)
    public Course getCourseById(Long id) {
        Optional<Course> course = courseRepository.findById(id);
        return course.orElse(null);
    }
    @Transactional(readOnly = true)
    public List<Course> getCoursesForLecturer(User lecturer) {
        return courseRepository.findByLecturer(lecturer);
    }
   

    public int countAllCourses() {
        return 24;
    }
    public List<Course> getTopCourses() {
        return courseRepository.findAll(); // simple version for now
    }
    
    @Transactional(readOnly = true)
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

     public void createCourse(Course course){
        courseRepository.save(course);
    }
    public Course updateCourse(Course course) {
        return courseRepository.save(course);
    }
    public void deleteCourse(Long id) {
        courseRepository.deleteById(id);
    }

    // Example: system health / course stats
    public String getSystemHealth(){
        return "All courses running fine"; // simple placeholder
    }
    public byte[] generateRosterReport(Course course) {
        Set<User> students = course.getStudents(); // ✅ now Set<User>

        StringBuilder csv = new StringBuilder();
        csv.append("Student Name,University ID,Email\n");

        for (User student : students) {
            csv.append(student.getFullName()).append(",");
            csv.append(student.getUniversityId()).append(",");
            csv.append(student.getEmail()).append("\n");
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }
    public void addCourseSchedule(Course course, User lecturer, LocalDateTime start, LocalDateTime end) {
        CourseSchedule schedule = new CourseSchedule();
        schedule.setCourse(course);
        schedule.setLecturer(lecturer);
        schedule.setStartTime(start);
        schedule.setEndTime(end);
        scheduleRepository.save(schedule);
    }

    public void updateCourseSchedule(Long courseId, Long scheduleId, User lecturer,
                                     LocalDateTime start, LocalDateTime end) {
        CourseSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Schedule not found"));

        if (!schedule.getCourse().getId().equals(courseId) || !schedule.getLecturer().equals(lecturer)) {
            throw new RuntimeException("Unauthorized");
        }

        schedule.setStartTime(start);
        schedule.setEndTime(end);
        scheduleRepository.save(schedule);
    }

    public void deleteCourseSchedule(Long courseId, Long scheduleId, User lecturer) {
        CourseSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Schedule not found"));

        if (!schedule.getCourse().getId().equals(courseId) || !schedule.getLecturer().equals(lecturer)) {
            throw new RuntimeException("Unauthorized");
        }

        scheduleRepository.delete(schedule);
    }
    public CourseSchedule getScheduleById(Long scheduleId) {
        return scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Schedule not found with ID: " + scheduleId));
    }

}