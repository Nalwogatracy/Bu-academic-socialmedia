package com.finalyearproject.controller;

import com.finalyearproject.model.Course;
import com.finalyearproject.model.Post;
import com.finalyearproject.model.User;
import com.finalyearproject.service.CourseService;
import com.finalyearproject.service.MessageService;
import com.finalyearproject.service.NotificationService;
import com.finalyearproject.service.PostService;
import com.finalyearproject.service.SystemSettingsService;
import com.finalyearproject.service.UserService;
import com.finalyearproject.service.UserStatusService;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class AdminController {

    @Autowired private UserService userService;
    @Autowired private CourseService courseService;
    @Autowired private PostService postService;
    @Autowired private SystemSettingsService systemSettingsService;
    @Autowired private MessageService messageService;
    @Autowired private NotificationService notificationService;
    @Autowired private UserStatusService userStatusService;
    @Autowired private SseController sseController;

    private void addSidebarData(Model model) {
        List<User> all = userService.getAllUsers();
        model.addAttribute("totalUsers",       all.size());
        model.addAttribute("activeUsers",      all.stream().filter(User::isApproved).count());
        model.addAttribute("pendingApprovals", userService.getPendingUsers().size());
        model.addAttribute("totalCourses",     courseService.getAllCourses().size());
    }

    // ═══════════════ DASHBOARD ═══════════════
    @GetMapping("/admin/dashboard")
    @Transactional(readOnly = true)
    public String dashboard(Model model) {
        model.addAttribute("user", userService.getLoggedInAdmin());
        addSidebarData(model);

        List<User> allUsers = userService.getAllUsers();
        model.addAttribute("totalStudents",
            allUsers.stream().filter(u -> u.getRole() != null && u.getRole().name().equals("STUDENT")).count());
        model.addAttribute("totalLecturers",
            allUsers.stream().filter(u -> u.getRole() != null && u.getRole().name().equals("LECTURER")).count());
        model.addAttribute("newUsersToday", allUsers.size());
        model.addAttribute("totalPosts", postService.getAllPosts().size());

        List<User> pendingUsers = userService.getPendingUsers();
        model.addAttribute("pendingUsers",     pendingUsers);
        model.addAttribute("pendingApprovals", pendingUsers.size());

        List<Course> courses = courseService.getAllCourses();
        model.addAttribute("courses",     courses);
        Map<Long, Integer> studentCounts = new HashMap<>();
        for (Course c : courses) {
            // students is Set<User>
            studentCounts.put(c.getId(), c.getStudents() != null ? c.getStudents().size() : 0);
        }
        List<User> lecturers = allUsers.stream()
                .filter(u -> u.getRole() != null && u.getRole().name().equals("LECTURER")).toList();
        model.addAttribute("lecturers",       lecturers);
        model.addAttribute("studentCounts", studentCounts);
        model.addAttribute("topCourses",   courseService.getTopCourses());
        model.addAttribute("totalCourses", courses.size());
        model.addAttribute("coursesWithPostCount", courses.stream()
            .map(c -> Map.of("course", c, "postCount", c.getPosts() != null ? c.getPosts().size() : 0))
            .collect(Collectors.toList()));
        

        return "admin-dashboard";
    }

    // ═══════════════ USERS ═══════════════
    @GetMapping("/admin/users")
    public String users(Model model, @RequestParam(defaultValue = "0") int page) {
        model.addAttribute("user", userService.getLoggedInAdmin());
        addSidebarData(model);
        Page<User> userPage = userService.getUsersPaged(PageRequest.of(page, 20));
        model.addAttribute("userPage", userPage);
        model.addAttribute("users", userPage.getContent());
        long total = userService.countAllUsers();
        model.addAttribute("studentCount", userService.countStudents());
        model.addAttribute("lecturerCount", userService.countLecturers());
        model.addAttribute("activeUsers", userService.countActiveUsers());
        return "admin-users";
    }

    // ═══════════════ COURSES ═══════════════
    @GetMapping("/admin/courses")
    @Transactional(readOnly = true)
    public String courses(Model model) {
        model.addAttribute("user", userService.getLoggedInAdmin());
        addSidebarData(model);

        List<User>   allUsers = userService.getAllUsers();
        List<Course> courses  = courseService.getAllCourses();
        model.addAttribute("courses",     courses);
        model.addAttribute("totalCourses", courses.size());

        List<User> lecturers = allUsers.stream()
                .filter(u -> u.getRole() != null && u.getRole().name().equals("LECTURER")).toList();
        model.addAttribute("lecturers",       lecturers);
        model.addAttribute("activeLecturers", lecturers.size());

        // students and materials are both Set — .size() works fine on Set
        long totalEnrollments = courses.stream()
                .mapToLong(c -> c.getStudents()  != null ? c.getStudents().size()  : 0).sum();
        long totalMaterials   = courses.stream()
                .mapToLong(c -> c.getMaterials() != null ? c.getMaterials().size() : 0).sum();
        model.addAttribute("totalEnrollments", totalEnrollments);
        model.addAttribute("totalMaterials",   totalMaterials);
        
        

        return "admin-courses";
    }

    // ═══════════════ ENROLLMENTS PAGE ═══════════════
    @GetMapping("/admin/courses/{id}/enrollments")
    @Transactional(readOnly = true)
    public String viewEnrollments(
            @PathVariable Long id,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "all") String status,
            Model model) {

        model.addAttribute("user", userService.getLoggedInAdmin());
        Course course = courseService.getCourseById(id);
        model.addAttribute("course", course);

        // course.getStudents() returns Set<User> — copy to List for filtering
        List<User> enrolled = new ArrayList<>(
                course.getStudents() != null ? course.getStudents() : Set.of());

        if (search != null && !search.isBlank()) {
            String q = search.toLowerCase();
            enrolled = enrolled.stream()
                    .filter(s -> s.getFullName().toLowerCase().contains(q)
                            || (s.getUniversityId() != null
                                && s.getUniversityId().toLowerCase().contains(q)))
                    .toList();
        }
        if (!status.equalsIgnoreCase("all")) {
            boolean active = status.equalsIgnoreCase("active");
            enrolled = enrolled.stream().filter(s -> s.isApproved() == active).toList();
        }

        model.addAttribute("students",       enrolled);
        model.addAttribute("totalStudents",  course.getStudents() != null ? course.getStudents().size() : 0);
        model.addAttribute("activeStudents", course.getStudents() != null
                ? course.getStudents().stream().filter(User::isApproved).count() : 0);

        // ── Available students ────────────────────────────────────────────
        // FIX: removed the broken faculty == department filter that caused
        // the empty list.  Now shows ALL approved students not yet enrolled.
        Set<Long> enrolledIds = course.getStudents() != null
                ? course.getStudents().stream().map(User::getId).collect(Collectors.toSet())
                : Set.of();

        List<User> available = userService.getAllUsers().stream()
                .filter(u -> u.getRole() != null && u.getRole().name().equals("STUDENT"))
                .filter(u -> u.isApproved())
                .filter(u -> !enrolledIds.contains(u.getId()))
                .sorted(Comparator.comparing(User::getFullName))
                .toList();

        model.addAttribute("availableStudents", available);
        return "admin-course-enrollments";
    }

    // ═══════════════ ENROLL STUDENTS ═══════════════
    /**
     * ManyToMany owner side is User.courses — must add course to
     * student.getCourses() and save the User, NOT course.getStudents().add().
     * Adding to the inverse side (Course.students) is silently ignored by JPA.
     */
    @PostMapping("/admin/courses/{id}/enroll")
    @Transactional
    public String enrollStudents(@PathVariable Long id,
                                 @RequestParam List<Long> studentIds) {
        Course course = courseService.getCourseById(id);
        for (Long studentId : studentIds) {
            User student = userService.getUserById(studentId);
            if (student.getCourses() == null) student.setCourses(new ArrayList<>());
            if (!student.getCourses().contains(course)) {
                student.getCourses().add(course);
                userService.saveUser(student);
            }
        }
        return "redirect:/admin/courses/" + id + "/enrollments";
    }

    // ═══════════════ REMOVE STUDENT ═══════════════
    @PostMapping("/admin/courses/{courseId}/students/{studentId}/remove")
    @Transactional
    public String removeStudent(@PathVariable Long courseId,
                                @PathVariable Long studentId) {
        Course course  = courseService.getCourseById(courseId);
        User   student = userService.getUserById(studentId);
        if (student.getCourses() != null) {
            student.getCourses().remove(course);
            userService.saveUser(student);
        }
        return "redirect:/admin/courses/" + courseId + "/enrollments";
    }

    // ═══════════════ REST JSON ═══════════════
    @GetMapping("/api/admin/courses/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCourseJson(@PathVariable Long id) {
        Course c = courseService.getCourseById(id);
        
        if (c == null) return ResponseEntity.notFound().build();
        int materialsCount = c.getMaterials() != null ? c.getMaterials().size() : 0;
        Map<String, Object> json = new HashMap<>();
        json.put("id",         c.getId());
        json.put("code",       c.getCode());
        json.put("name",       c.getName());
        json.put("department", c.getDepartment());
        json.put("materialsCount", materialsCount);
        if (c.getLecturer() != null) json.put("lecturerId", c.getLecturer().getId());
        return ResponseEntity.ok(json);
    }

    // ═══════════════ COURSE CRUD ═══════════════
    @GetMapping("/admin/courses/{id}")
    public String viewCourse(@PathVariable Long id, Model model) {
        model.addAttribute("user",   userService.getLoggedInAdmin());
        model.addAttribute("course", courseService.getCourseById(id));
        return "admin-course-view";
    }

    @GetMapping("/admin/courses/{id}/materials")
    public String manageMaterials(@PathVariable Long id, Model model) {
        model.addAttribute("user", userService.getLoggedInAdmin());
        Course course = courseService.getCourseById(id);
        model.addAttribute("course",    course);
        model.addAttribute("materials", course.getMaterials()); // Set<Material> — fine in Thymeleaf
        return "admin-course-materials";
    }

    @PostMapping("/admin/courses/add")
    public String addCourse(@ModelAttribute Course course,
                            @RequestParam(required = false) Long lecturerId) {
        if (lecturerId != null) course.setLecturer(userService.getUserById(lecturerId));
        courseService.createCourse(course);
        return "redirect:/admin/courses";
    }

    @PostMapping("/admin/courses/update")
    public String updateCourse(@ModelAttribute Course course,
                               @RequestParam(required = false) Long lecturerId) {
        if (lecturerId != null) course.setLecturer(userService.getUserById(lecturerId));
        courseService.updateCourse(course);
        return "redirect:/admin/courses";
    }

    @PostMapping("/admin/courses/{id}/delete")
    public String deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return "redirect:/admin/courses";
    }

    // ═══════════════ MESSAGES ═══════════════
    @GetMapping("/admin/messages")
    @Transactional(readOnly = true)
    public String messages(Model model, Authentication authentication) {
        User admin = userService.findByEmail(authentication.getName());

        Set<Long> onlineUserIds = messageService.getConversationsForUser(admin)
            .keySet().stream()
            .filter(u -> userStatusService.isOnline(u))
            .map(User::getId)
            .collect(Collectors.toSet());

        Map<Long, Integer> unreadPerUser = new HashMap<>();
        messageService.getConversationsForUser(admin).keySet().forEach(u ->
            unreadPerUser.put(u.getId(), messageService.countUnreadFromUser(u, admin))
        );

        model.addAttribute("onlineUserIds", onlineUserIds);
        model.addAttribute("unreadPerUser", unreadPerUser);
        model.addAttribute("user", admin);
        model.addAttribute("other", null);
        model.addAttribute("conversations", messageService.getConversationsForUser(admin));
        model.addAttribute("unreadMessages", messageService.countUnread(admin));
        model.addAttribute("unreadNotifications", notificationService.countUnread(admin));
        model.addAttribute("allUsersExceptCurrent", userService.getAllUsersExcept(admin));

        return "admin-messages";
    }

    @GetMapping("/admin/messages/{userId}")
    @Transactional
    public String conversation(@PathVariable Long userId, Model model,
                               Authentication authentication) {
        User admin = userService.findByEmail(authentication.getName());
        User other = userService.getUserById(userId);

        messageService.markConversationAsRead(admin, other);

        Set<Long> onlineUserIds = messageService.getConversationsForUser(admin)
            .keySet().stream()
            .filter(u -> userStatusService.isOnline(u))
            .map(User::getId)
            .collect(Collectors.toSet());

        Map<Long, Integer> unreadPerUser = new HashMap<>();
        messageService.getConversationsForUser(admin).keySet().forEach(u ->
            unreadPerUser.put(u.getId(), messageService.countUnreadFromUser(u, admin))
        );

        model.addAttribute("onlineUserIds", onlineUserIds);
        model.addAttribute("conversations", messageService.getConversationsForUser(admin));
        model.addAttribute("unreadPerUser", unreadPerUser);
        model.addAttribute("user", admin);
        model.addAttribute("other", other);
        model.addAttribute("messages", messageService.getConversation(admin, other));
        model.addAttribute("unreadMessages", messageService.countUnread(admin));
        model.addAttribute("unreadNotifications", notificationService.countUnread(admin));
        model.addAttribute("isOtherOnline", userStatusService.isOnline(other));
        model.addAttribute("allUsersExceptCurrent", userService.getAllUsersExcept(admin));

        return "admin-conversation";
    }

    @GetMapping("/admin/messages/{id}/refresh")
    @ResponseBody
    public String refresh(@PathVariable Long id, Authentication auth, Model model) {
        User admin = userService.findByEmail(auth.getName());
        User other = userService.getUserById(id);
        model.addAttribute("messages", messageService.getConversation(admin, other));
        return "fragments/message-list :: messages";
    }

    @PostMapping("/admin/messages/{id}/read")
    @ResponseBody
    public void markRead(@PathVariable Long id, Authentication authentication) {
        User admin = userService.findByEmail(authentication.getName());
        User other = userService.getUserById(id);
        messageService.markConversationAsRead(admin, other);
    }

    @PostMapping("/admin/messages/typing")
    @ResponseBody
    public void typing(@RequestBody Map<String, Object> body,
                       Authentication authentication) {
        User admin = userService.findByEmail(authentication.getName());
        Long recipientId = Long.valueOf(body.get("recipientId").toString());
        sseController.pushTyping(recipientId, admin.getId());
    }

    @PostMapping("/admin/messages/send")
    public String sendMessage(Authentication authentication,
                              @RequestParam Long recipientId,
                              @RequestParam(required = false, defaultValue = "Message") String subject,
                              @RequestParam String content,
                              RedirectAttributes redirectAttributes) {
        User admin = userService.findByEmail(authentication.getName());
        User recipient = userService.getUserById(recipientId);
        messageService.sendMessage(admin, recipient, content, subject);
        redirectAttributes.addFlashAttribute("success", "Message sent successfully");
        return "redirect:/admin/messages/" + recipientId;
    }

    // ═══════════════ NOTIFICATIONS ═══════════════
    @GetMapping("/admin/notifications")
    @Transactional(readOnly = true)
    public String notifications(Model model, Authentication authentication) {
        User admin = userService.findByEmail(authentication.getName());
        model.addAttribute("notifications", notificationService.getNotificationsForUser(admin));
        model.addAttribute("user", admin);
        model.addAttribute("unreadMessages", messageService.countUnread(admin));
        model.addAttribute("unreadNotifications", notificationService.countUnread(admin));
        return "admin-notifications";
    }

    @PostMapping("/admin/notifications/mark-read")
    @ResponseBody
    public String markNotificationsRead(Authentication authentication) {
        User admin = userService.findByEmail(authentication.getName());
        notificationService.markAllAsRead(admin);
        return "ok";
    }

    // ═══════════════ USER CRUD ═══════════════
    @PostMapping("/admin/users/{id}/approve")
    public String approveUser(@PathVariable Long id) { userService.approveUser(id); return "redirect:/admin/users"; }
    @PostMapping("/admin/users/{id}/reject")
    public String rejectUser(@PathVariable Long id)  { userService.rejectUser(id);  return "redirect:/admin/users"; }
    @PostMapping("/admin/users/{id}/suspend")
    public String suspendUser(@PathVariable Long id) { userService.suspendUser(id); return "redirect:/admin/users"; }
    @PostMapping("/admin/users/{id}/delete")
    public String deleteUser(@PathVariable Long id)  { userService.deleteUser(id);  return "redirect:/admin/users"; }

    @PostMapping("/admin/users/add")
    public String addUser(@ModelAttribute User user) {
        userService.createUser(user); return "redirect:/admin/users";
    }
    @PostMapping("/admin/users/update")
    public String updateUser(@ModelAttribute User user,
                             @RequestParam(required = false) String status) {
        if ("ACTIVE".equals(status))   user.setApproved(true);
        else if (status != null)       user.setApproved(false);
        userService.updateUser(user);
        return "redirect:/admin/users";
    }
    @GetMapping("/admin/users/{id}")
    public String viewUser(@PathVariable Long id, Model model) {
        model.addAttribute("user", userService.getLoggedInAdmin());
        User viewed = userService.getUserById(id);
        if (viewed == null) return "redirect:/admin/users?error=notfound";
        model.addAttribute("viewedUser", viewed);
        return "admin-user-view";
    }

    // ═══════════════ OTHER PAGES ═══════════════
    @GetMapping("/admin/posts")
    public String posts(Model model) {
        model.addAttribute("user",  userService.getLoggedInAdmin());
        addSidebarData(model);
        model.addAttribute("posts", postService.getAllPosts());
        return "admin-posts";
    }
    @GetMapping("/admin/analytics")
    public String analytics(Model model) {
        model.addAttribute("user", userService.getLoggedInAdmin());
        addSidebarData(model);
        model.addAttribute("userStats", userService.getUserStatistics());
        model.addAttribute("postStats", postService.getPostStatistics());
        return "admin-analytics";
    }
    @GetMapping("/admin/reports")
    public String reports(Model model) {
        model.addAttribute("user", userService.getLoggedInAdmin());
        addSidebarData(model);
        model.addAttribute("reports", List.of());
        return "admin-reports";
    }
    @GetMapping("/admin/system")
    public String system(Model model) {
        model.addAttribute("user",         userService.getLoggedInAdmin());
        addSidebarData(model);
        model.addAttribute("systemHealth", courseService.getSystemHealth());
        model.addAttribute("settings",     systemSettingsService.getSettings());
        return "admin-system";
    }
    @GetMapping("/admin/logs")
    public String logs(Model model) {
        model.addAttribute("user", userService.getLoggedInAdmin());
        addSidebarData(model);
        model.addAttribute("logs", List.of());
        return "admin-logs";
    }
    @PostMapping("/admin/logout")
    public String logout() { return "redirect:/login"; }
    
    @PostMapping("/admin/users/{id}/reset-password")
    @Transactional
    public String resetUserPassword(@PathVariable Long id) {
        User user = userService.getUserById(id);
        if (user != null) {
            // Generate a new temporary password
            String tempPassword = userService.generateTemporaryPassword();
            // Update the user password (hashed)
            userService.updatePassword(user, tempPassword);
            // Optionally, send email notification
            userService.sendPasswordResetEmail(user, tempPassword);
        }
        return "redirect:/admin/users/" + id + "?reset=success";
    }
    @PostMapping("/user/upload-avatar")
    @ResponseBody
    @Transactional
    public Map<String, Object> uploadProfilePicture(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        try {
            User user = userService.findByEmail(authentication.getName());
            user.setProfilePicture(file.getBytes());
            user.setProfilePictureType(file.getContentType());
            userService.saveUser(user);
            return Map.of("success", true);
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }
    
    @GetMapping("/user/profile-picture/{id}")
    @Transactional(readOnly = true)
    @ResponseBody
    public ResponseEntity<byte[]> getProfilePicture(@PathVariable Long id) {
        User user = userService.getUserById(id);
        if (user.getProfilePicture() == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
            .header("Content-Type", user.getProfilePictureType() != null 
                ? user.getProfilePictureType() : "image/jpeg")
            .header("Cache-Control", "max-age=3600") // cache for 1 hour
            .body(user.getProfilePicture());
    }
    
}