package com.finalyearproject.service;

import com.finalyearproject.model.Attendance;
import com.finalyearproject.model.Course;
import com.finalyearproject.model.User;
import com.finalyearproject.repository.AttendanceRepository;
import com.finalyearproject.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final CourseService courseService;
    private final UserRepository userRepository;

    public AttendanceService(AttendanceRepository attendanceRepository, CourseService courseService, UserRepository userRepository) {
        this.attendanceRepository = attendanceRepository;
        this.courseService = courseService;
        this.userRepository = userRepository;
    }

    @Transactional
    public void markAttendance(User student, Course course, User markedBy, String status, LocalDate date) {
        List<Attendance> existing = attendanceRepository.findByCourseAndDate(course, date);
        for (Attendance a : existing) {
            if (a.getStudent().getId().equals(student.getId())) {
                attendanceRepository.delete(a);
                break;
            }
        }

        Attendance attendance = new Attendance();
        attendance.setStudent(student);
        attendance.setCourse(course);
        attendance.setMarkedBy(markedBy);
        attendance.setStatus(status);
        attendance.setDate(date);
        attendance.setCreatedAt(LocalDateTime.now());
        attendanceRepository.save(attendance);
    }

    @Transactional
    public void saveAttendanceBatch(List<Long> studentIds, Long courseId, User markedBy, String dateStr, List<String> statuses) {
        Course course = courseService.getCourseById(courseId);
        LocalDate date = LocalDate.parse(dateStr);
        for (int i = 0; i < studentIds.size(); i++) {
            Long sid = studentIds.get(i);
            User student = userRepository.findById(sid)
                .orElseThrow(() -> new RuntimeException("Student not found: " + sid));
            markAttendance(student, course, markedBy, statuses.get(i), date);
        }
    }

    public List<Attendance> getAttendanceForCourseAndDate(Course course, LocalDate date) {
        return attendanceRepository.findByCourseAndDate(course, date);
    }

    public Map<String, Long> getStatsForCourseAndDate(Course course, LocalDate date) {
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("PRESENT", 0L);
        stats.put("ABSENT", 0L);
        stats.put("LATE", 0L);
        stats.put("EXCUSED", 0L);
        List<Object[]> counts = attendanceRepository.getStatusCountsForCourseAndDate(course, date);
        for (Object[] row : counts) {
            stats.put((String) row[0], (Long) row[1]);
        }
        return stats;
    }

    public double getAttendancePercentage(User student, Course course) {
        long total = attendanceRepository.findByStudentAndCourse(student, course).size();
        if (total == 0) return 0;
        long present = attendanceRepository.countByStudentAndCourseAndStatus(student, course, "PRESENT");
        long late = attendanceRepository.countByStudentAndCourseAndStatus(student, course, "LATE");
        return ((double) (present + late) / total) * 100;
    }

    public byte[] generateAttendanceReport(Course course) {
        List<Attendance> records = attendanceRepository.findByCourse(course);
        Set<User> students = course.getStudents();

        StringBuilder csv = new StringBuilder();
        csv.append("Student Name,University ID,Email");

        // Collect distinct dates sorted
        List<LocalDate> dates = records.stream()
                .map(Attendance::getDate)
                .distinct()
                .sorted()
                .toList();
        for (LocalDate d : dates) {
            csv.append(",").append(d.toString());
        }
        csv.append("\n");

        for (User student : students) {
            csv.append(student.getFullName()).append(",");
            csv.append(student.getUniversityId() != null ? student.getUniversityId() : "").append(",");
            csv.append(student.getEmail());

            for (LocalDate d : dates) {
                String status = records.stream()
                        .filter(r -> r.getStudent().getId().equals(student.getId()) && r.getDate().equals(d))
                        .map(Attendance::getStatus)
                        .findFirst()
                        .orElse("");
                csv.append(",").append(status);
            }
            csv.append("\n");
        }

        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
}
