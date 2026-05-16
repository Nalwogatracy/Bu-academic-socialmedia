package com.finalyearproject.service;

import com.finalyearproject.model.*;
import com.finalyearproject.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CalendarService {

    private final CourseRepository courseRepository;
    private final AssignmentRepository assignmentRepository;
    private final QuizRepository quizRepository;
    private final CourseScheduleRepository scheduleRepository;
    private final DeadlineRepository deadlineRepository;

    public CalendarService(CourseRepository courseRepository,
                           AssignmentRepository assignmentRepository,
                           QuizRepository quizRepository,
                           CourseScheduleRepository scheduleRepository,
                           DeadlineRepository deadlineRepository) {
        this.courseRepository = courseRepository;
        this.assignmentRepository = assignmentRepository;
        this.quizRepository = quizRepository;
        this.scheduleRepository = scheduleRepository;
        this.deadlineRepository = deadlineRepository;
    }

    public List<CalendarEvent> getEventsForStudent(User student) {
        List<Course> courses = courseRepository.findCoursesWithDetailsByStudent(student);
        List<CalendarEvent> events = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (Course course : courses) {
            // Assignments
            List<Assignment> assignments = assignmentRepository.findByCourse(course);
            for (Assignment a : assignments) {
                if (a.getDueDate() != null && a.getDueDate().isAfter(now.minusDays(1))) {
                    events.add(new CalendarEvent(
                            a.getTitle(), a.getDueDate(), CalendarEvent.EventType.ASSIGNMENT,
                            course.getName(), course.getCode(), course.getId(),
                            "/student/assignment/" + a.getId() + "/submit", "#ef4444"
                    ));
                }
            }

            // Quizzes
            List<Quiz> quizzes = quizRepository.findByCourse(course);
            for (Quiz q : quizzes) {
                if (q.getDueDate() != null && q.getDueDate().isAfter(now.minusDays(1))) {
                    events.add(new CalendarEvent(
                            q.getTitle(), q.getDueDate(), CalendarEvent.EventType.QUIZ,
                            course.getName(), course.getCode(), course.getId(),
                            "/student/quizzes/" + q.getId() + "/start", "#8b5cf6"
                    ));
                }
            }

            // Schedules
            List<CourseSchedule> schedules = scheduleRepository.findByCourse(course);
            for (CourseSchedule s : schedules) {
                if (s.getStartTime() != null && s.getStartTime().isAfter(now.minusDays(1))) {
                    events.add(new CalendarEvent(
                            course.getName() + " Class", s.getStartTime(), s.getEndTime(),
                            CalendarEvent.EventType.SCHEDULE,
                            course.getName(), course.getCode(), course.getId(),
                            "/student/course/" + course.getId(), "#22c55e"
                    ));
                }
            }
        }

        // Deadlines
        List<Deadline> deadlines = deadlineRepository.findUpcomingForUser(student);
        for (Deadline d : deadlines) {
            events.add(new CalendarEvent(
                    d.getTitle(), d.getDueDate(), CalendarEvent.EventType.DEADLINE,
                    d.getCourse() != null ? d.getCourse().getName() : "",
                    d.getCourse() != null ? d.getCourse().getCode() : "",
                    d.getCourse() != null ? d.getCourse().getId() : null,
                    "#",
                    d.getColorCode() != null ? d.getColorCode() : "#f59e0b"
            ));
        }

        events.sort(Comparator.comparing(CalendarEvent::getDateTime));
        return events;
    }

    public List<CalendarEvent> getEventsForLecturer(User lecturer) {
        List<Course> courses = courseRepository.findByLecturer(lecturer);
        List<CalendarEvent> events = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (Course course : courses) {
            List<Assignment> assignments = assignmentRepository.findByCourse(course);
            for (Assignment a : assignments) {
                if (a.getDueDate() != null && a.getDueDate().isAfter(now.minusDays(1))) {
                    events.add(new CalendarEvent(
                            a.getTitle(), a.getDueDate(), CalendarEvent.EventType.ASSIGNMENT,
                            course.getName(), course.getCode(), course.getId(),
                            "/lecturer/assignment/" + a.getId() + "/submissions", "#ef4444"
                    ));
                }
            }

            List<Quiz> quizzes = quizRepository.findByCourse(course);
            for (Quiz q : quizzes) {
                if (q.getDueDate() != null && q.getDueDate().isAfter(now.minusDays(1))) {
                    events.add(new CalendarEvent(
                            q.getTitle(), q.getDueDate(), CalendarEvent.EventType.QUIZ,
                            course.getName(), course.getCode(), course.getId(),
                            "/lecturer/quizzes/" + q.getId() + "/submissions", "#8b5cf6"
                    ));
                }
            }

            List<CourseSchedule> schedules = scheduleRepository.findByCourse(course);
            for (CourseSchedule s : schedules) {
                if (s.getStartTime() != null && s.getStartTime().isAfter(now.minusDays(1))) {
                    events.add(new CalendarEvent(
                            course.getName() + " Class", s.getStartTime(), s.getEndTime(),
                            CalendarEvent.EventType.SCHEDULE,
                            course.getName(), course.getCode(), course.getId(),
                            "/lecturer/course/" + course.getId(), "#22c55e"
                    ));
                }
            }
        }

        events.sort(Comparator.comparing(CalendarEvent::getDateTime));
        return events;
    }

    public List<CalendarEvent> getEventsForAdmin() {
        List<Course> courses = courseRepository.findAll();
        List<CalendarEvent> events = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (Course course : courses) {
            List<Assignment> assignments = assignmentRepository.findByCourse(course);
            for (Assignment a : assignments) {
                if (a.getDueDate() != null && a.getDueDate().isAfter(now.minusDays(1))) {
                    events.add(new CalendarEvent(
                            a.getTitle(), a.getDueDate(), CalendarEvent.EventType.ASSIGNMENT,
                            course.getName(), course.getCode(), course.getId(),
                            "/admin/courses/" + course.getId(), "#ef4444"
                    ));
                }
            }
        }

        events.sort(Comparator.comparing(CalendarEvent::getDateTime));
        return events;
    }
}
