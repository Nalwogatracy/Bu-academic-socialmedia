package com.finalyearproject.controller;

import com.finalyearproject.model.*;
import com.finalyearproject.repository.AttachmentRepository;
import com.finalyearproject.repository.CourseRepository;
import com.finalyearproject.repository.MessageRepository;
import com.finalyearproject.repository.PostRepository;
import com.finalyearproject.service.*;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Enumeration;
import org.hibernate.Hibernate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;   
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/student")
public class StudentController {

    private static final Logger log = LoggerFactory.getLogger(StudentController.class);

    private final UserService userService;
    private final CourseService courseService;
    private final MaterialService materialService;
    private final PostService postService;
    private final DeadlineService deadlineService;
    private final UserStatusService userStatusService;
    private final EngagementService engagementService;
    private final MessageService messageService;
    private final NotificationService notificationService;
    private final AssignmentService assignmentService;
    private final SavedService savedService;
    private final DashboardService dashboardService;
    private final DiscussionService discussionService;
    private final StudyGroupService studyGroupService;
    private final FileStorageService fileStorageService;
    private final AttachmentRepository attachmentRepository;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private QuestionService questionService;
    @Autowired
    private QuizService quizService;
    @Autowired
    private QuizAttemptService quizAttemptService;

    public StudentController(UserService userService,
                             CourseService courseService,
                             MaterialService materialService,
                             PostService postService,
                             DeadlineService deadlineService,
                             UserStatusService userStatusService,
                             EngagementService engagementService,
                             MessageService messageService,
                             NotificationService notificationService,
                             AssignmentService assignmentService,
                             SavedService savedService,
                             DashboardService dashboardService,
                             DiscussionService discussionService,
                             FileStorageService fileStorageService,
                             AttachmentRepository attachmentRepository,
                             StudyGroupService studyGroupService) {
        this.userService = userService;
        this.courseService = courseService;
        this.materialService = materialService;
        this.postService = postService;
        this.deadlineService = deadlineService;
        this.userStatusService = userStatusService;
        this.engagementService = engagementService;
        this.messageService = messageService;
        this.notificationService = notificationService;
        this.assignmentService = assignmentService;
        this.savedService = savedService;
        this.dashboardService = dashboardService;
        this.discussionService = discussionService;
        this.studyGroupService = studyGroupService;
        this.fileStorageService = fileStorageService;
        this.attachmentRepository = attachmentRepository;
    }
    
    @GetMapping("/dashboard")
    @Transactional(readOnly = true)
    public String studentDashboard(Model model,
                                   Authentication authentication,
                                   @RequestParam(defaultValue = "ALL") String filter) {

        String email = authentication.getName();
        User user = userService.findByEmail(email);

        // Stats
        int coursesCount = courseService.countCoursesForUser(user);
        List<SavedItem> savedList = savedService != null ? savedService.getSavedItemsForUser(user) : List.of();
        model.addAttribute("savedItemsList", savedList);

        int materialsCount = materialService.countMaterialsForUser(user);
        double engagement = userService.calculateEngagement(user);

        model.addAttribute("engagementPercentage", (int) engagement + "%");
        model.addAttribute("user", user);
        model.addAttribute("coursesCount", coursesCount);
        model.addAttribute("materialsCount", materialsCount);
        model.addAttribute("activeFilter", filter);

        // Courses
        List<Course> courses = courseService.getCoursesForUser(user);
        model.addAttribute("courses", courses);
        

        // Materials from those courses
        List<Material> materials = materialService.getMaterialsForCourses(courses);
        model.addAttribute("materials", materials);

        // Posts from lecturer for those courses
        List<Post> filteredPosts = postRepository.findByCourseIn(courses)
                .stream()
                .filter(p -> {
                    if (filter.equals("ALL")) return true;

                    // "From Lecturers" button — filter by author role, not post type
                    if (filter.equals("LECTURER"))
                        return p.getAuthor() != null &&
                               p.getAuthor().getRole() != null &&
                               p.getAuthor().getRole().name().equals("LECTURER");

                    // All other buttons (ASSIGNMENT, MATERIAL, DISCUSSION) — filter by post type
                    return p.getType() != null && p.getType().equalsIgnoreCase(filter);
                })
                .sorted(Comparator.comparing(Post::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

            model.addAttribute("posts", filteredPosts); // ← was sending unfiltered 'posts' before

        // Deadlines
        List<Deadline> deadlines = deadlineService.getUpcomingDeadlinesForUser(user);
        model.addAttribute("deadlines", deadlines);

        // Peers
        List<UserStatus> peersOnline = userStatusService.getOnlinePeersForUser(user);
        model.addAttribute("peersOnline", peersOnline);

        // Quiz stats
        int totalQuizzes = 0;
        int completedQuizzes = 0;
        int totalQuizScore = 0;
        int totalQuizPoints = 0;
        for (Course course : courses) {
            List<Quiz> courseQuizzes = quizService.getQuizzesForCourse(course);
            for (Quiz quiz : courseQuizzes) {
                totalQuizzes++;
                List<QuizAttempt> attempts = quizAttemptService.getAttemptsForStudent(quiz, user);
                for (QuizAttempt attempt : attempts) {
                    if ("GRADED".equals(attempt.getStatus())) {
                        completedQuizzes++;
                        totalQuizScore += attempt.getScore();
                        totalQuizPoints += attempt.getTotalPoints();
                    }
                }
            }
        }
        model.addAttribute("totalQuizzes", totalQuizzes);
        model.addAttribute("completedQuizzes", completedQuizzes);
        model.addAttribute("avgQuizScore", totalQuizPoints > 0 ? (totalQuizScore * 100 / totalQuizPoints) : 0);

        // Badges
        int unreadMessages = messageService != null ? messageService.countUnread(user) : 0;
        int unreadNotifications = notificationService != null ? notificationService.countUnread(user) : 0;
        int pendingAssignments = assignmentService != null ? assignmentService.countPending(user) : 0;
        int savedItems = savedService != null ? savedService.countSaved(user) : 0;

        model.addAttribute("unreadMessages", unreadMessages);
        model.addAttribute("unreadNotifications", unreadNotifications);
        model.addAttribute("pendingAssignments", pendingAssignments);
        model.addAttribute("savedItems", savedItems);

        return "student-dashboard";
    }
    // My Courses
    @GetMapping("/courses")
    @Transactional(readOnly = true)
    public String myCourses(Authentication authentication, Model model) {
        if (authentication == null || authentication.getName() == null) {
            return "redirect:/login"; // not logged in
        }

        String email = authentication.getName();
        User user = userService.findByEmail(email); // fetch from DB

        dashboardService.populateDashboardModel(user, model);
        return "dashboard-courses";
    }

    // Assignments
    @GetMapping("/assignments")
    @Transactional(readOnly = true)
    public String assignments(Authentication authentication, Model model) {
        String email = authentication.getName();
        User user = userService.findByEmail(email);

        // Base data for sidebar/navbar
        List<Course> courses = courseService.getCoursesForUser(user);
        model.addAttribute("user", user);
        model.addAttribute("courses", courses);
        model.addAttribute("coursesCount", courses.size());
        model.addAttribute("materialsCount", materialService.countMaterialsForUser(user));
        model.addAttribute("engagementPercentage", (int) userService.calculateEngagement(user) + "%");
        model.addAttribute("unreadMessages", messageService.countUnread(user));
        model.addAttribute("unreadNotifications", notificationService.countUnread(user));
        model.addAttribute("pendingAssignments", assignmentService.countPending(user));
        model.addAttribute("savedItems", savedService != null ? savedService.countSaved(user) : 0);
        model.addAttribute("deadlines", deadlineService.getUpcomingDeadlinesForUser(user));

        // ── Load all assignments for the student's courses ────────────────────
        List<Assignment> allAssignments = courses.stream()
            .flatMap(c -> assignmentService.getCourseAssignments(c.getId()).stream())
            .collect(Collectors.toList());

        // ── Categorize by status ──────────────────────────────────────────────
        List<Assignment> pending = allAssignments.stream()
            .filter(a -> a.getDueDate().isAfter(LocalDateTime.now()))
            .collect(Collectors.toList());

        List<Assignment> submitted = allAssignments.stream()
            .filter(a -> assignmentService.hasStudentSubmitted(a, user))
            .collect(Collectors.toList());

        List<Assignment> graded = allAssignments.stream()
            .filter(a -> assignmentService.isGradedForStudent(a, user))
            .collect(Collectors.toList());

        model.addAttribute("assignments", allAssignments);
        model.addAttribute("totalAssignments", allAssignments.size());
        model.addAttribute("pendingAssignments", pending.size());
        model.addAttribute("submittedAssignments", submitted.size());
        model.addAttribute("gradedAssignments", graded.size());

        return "dashboard-assignments";
    }

    // Discussions
    @GetMapping("/discussions")
    @Transactional(readOnly = true)
    public String discussions(Authentication authentication, Model model,
            @RequestParam(name = "course", required = false) Long courseId,
            @RequestParam(name = "filter", required = false, defaultValue = "ALL") String filter) {
        if (authentication == null) return "redirect:/login";

        String email = authentication.getName();
        User user = userService.findByEmail(email);

        dashboardService.populateDashboardModel(user, model);

        // ── User's own courses (for sidebar + dropdown) ──────────────────────
        List<Course> userCourses = courseService.getCoursesForUser(user);
        model.addAttribute("courses", userCourses);         // ← fixes sidebar/dropdown
        model.addAttribute("activeFilter", filter);         // ← fixes filter tab highlighting
        model.addAttribute("selectedCourse", courseId);     // ← fixes course dropdown selection

        // ── Unread counts (only for user's courses) ──────────────────────────
        Map<Long, Integer> unreadCounts = new HashMap<>();
        for (Course course : userCourses) {
            int count = messageRepository.countUnreadByConversation(user, course.getId());
            unreadCounts.put(course.getId(), count);
        }
        model.addAttribute("unreadCounts", unreadCounts);

        // ── Coursemates (deduplicated by ID) ─────────────────────────────────
        List<User> coursemates = userCourses.stream()
            .flatMap(c -> c.getStudents() != null
                    ? c.getStudents().stream()
                    : java.util.stream.Stream.empty())
            .filter(u -> !u.getId().equals(user.getId()))
            .collect(Collectors.toMap(
                User::getId,
                u -> u,
                (a, b) -> a
            ))
            .values()
            .stream()
            .collect(Collectors.toList());
        model.addAttribute("allCoursemates", coursemates);

        // ── Filter topics ────────────────────────────────────────────────────
        List<Question> allTopics = questionService.getAllQuestionsForUser(user);

        List<Question> filtered = allTopics.stream()
            .filter(q -> {
                if (courseId != null) {
                    return q.getCourse() != null && q.getCourse().getId().equals(courseId);
                }
                return true;
            })
            .filter(q -> switch (filter) {
                case "POPULAR"    -> q.isPopular();
                case "UNANSWERED" -> !q.isAnswered();
                case "PINNED"     -> q.isPinned();
                case "MINE"       -> q.getAuthor() != null &&
                                     q.getAuthor().getId().equals(user.getId());
                default           -> true;
            })
            .collect(Collectors.toList());

        model.addAttribute("pinnedTopics",
            filtered.stream().filter(Question::isPinned).collect(Collectors.toList()));
        model.addAttribute("discussionTopics",
            filtered.stream().filter(q -> !q.isPinned()).collect(Collectors.toList()));

        // ── Sidebar extras ───────────────────────────────────────────────────
        model.addAttribute("myQuestions", questionService.getUserQuestions(user));
        model.addAttribute("popularTopics", questionService.getPopularQuestions());

        return "dashboard-discussions";
    }
    
    @PostMapping("/discussions/create")
    public String createDiscussion(Authentication authentication,
                                   @RequestParam String title,
                                   @RequestParam String content,
                                   @RequestParam(required = false) Long courseId,
                                   @RequestParam(required = false) String type,
                                   @RequestParam(required = false) List<Long> memberIds,
                                   RedirectAttributes redirectAttributes) {
        User user = userService.findByEmail(authentication.getName());

        Question question = new Question();
        question.setTitle(title);
        question.setContent(content);
        question.setAuthor(user);
        question.setCreatedAt(LocalDateTime.now());

        // Set course if provided
        if (courseId != null) {
            Course course = courseService.getCourseById(courseId);
            if (course != null) {
                question.setCourse(course);
            }
        }

        // Set private group members if type is GROUP
        if ("GROUP".equals(type) && memberIds != null && !memberIds.isEmpty()) {
            question.setPrivateGroup(true);
            question.setGroupMembers(memberIds.stream()
                .map(userService::getUserById)
                .collect(Collectors.toList()));
        }

        questionService.saveQuestion(question);
        redirectAttributes.addFlashAttribute("success", "Discussion started!");
        return "redirect:/student/discussions";
    }

    // Study Groups
    @GetMapping("/study-groups")
    public String studyGroups(Authentication authentication, Model model) {
        if (authentication == null) return "redirect:/login";

        User user = userService.findByEmail(authentication.getName());

        dashboardService.populateDashboardModel(user, model);
        model.addAttribute("myGroups", studyGroupService.getUserGroups(user));
        model.addAttribute("recommendedGroups", studyGroupService.getRecommendedGroups(user));
        model.addAttribute("upcomingSessions", studyGroupService.getUpcomingSessions(user));
        model.addAttribute("activeUsers", userStatusService.getActiveStudyUsers());
        return "dashboard-study-groups";
    }

    // Downloads
    @GetMapping("/downloads")
    public String downloads(Authentication authentication, Model model) {
        if (authentication == null) return "redirect:/login";

        User user = userService.findByEmail(authentication.getName());

        dashboardService.populateDashboardModel(user, model);
        return "dashboard-downloads";
    }

    // Saved Items
    @GetMapping("/saved")
    public String saved(Authentication authentication, Model model) {
        if (authentication == null) return "redirect:/login";

        User user = userService.findByEmail(authentication.getName());

        dashboardService.populateDashboardModel(user, model);
        List<Post> savedPosts = postService.getSavedPostsForUser(user);
    
        model.addAttribute("savedPosts", savedPosts);
        model.addAttribute("savedItems", savedPosts.size());
        model.addAttribute("recentlySaved", savedPosts.stream()
            .limit(5)
            .collect(Collectors.toList()));
        return "dashboard-saved";
    }

    // Grades
    @GetMapping("/grades")
    @Transactional(readOnly = true)
    public String grades(Model model, Authentication authentication) {
        User student = userService.findByEmail(authentication.getName());
        List<Course> courses = student.getCourses();

        Map<Course, List<Submission>> courseSubmissions = new LinkedHashMap<>();
        Map<Course, Double> courseAverages = new LinkedHashMap<>();
        int totalGraded = 0;
        int totalUngraded = 0;

        for (Course course : courses) {
            List<Submission> submissions = assignmentService.getSubmissionsForStudent(course, student);
            courseSubmissions.put(course, submissions);
            courseAverages.put(course, assignmentService.getAverageScoreForCourse(course));

            for (Submission s : submissions) {
                if (s.getGraded()) totalGraded++;
                else totalUngraded++;
            }
        }

        double overallAverage = courseAverages.values().stream()
            .filter(Objects::nonNull)
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);

        model.addAttribute("user", student);
        model.addAttribute("courses", courses);
        model.addAttribute("courseSubmissions", courseSubmissions);
        model.addAttribute("courseAverages", courseAverages);
        model.addAttribute("overallAverage", overallAverage);
        model.addAttribute("totalGraded", totalGraded);
        model.addAttribute("totalUngraded", totalUngraded);
        model.addAttribute("unreadMessages", messageService.countUnread(student));
        model.addAttribute("unreadNotifications", notificationService.countUnread(student));
        model.addAttribute("coursesCount", courses.size());
        model.addAttribute("materialsCount", materialService.countMaterialsForUser(student));
        model.addAttribute("engagementPercentage", (int) userService.calculateEngagement(student) + "%");
        model.addAttribute("pendingAssignments", assignmentService.countPending(student));
        model.addAttribute("savedItems", savedService != null ? savedService.countSaved(student) : 0);
        model.addAttribute("deadlines", deadlineService.getUpcomingDeadlinesForUser(student));

        return "student-grades";
    }

    // Progress
    @GetMapping("/progress")
    public String progress(Authentication authentication, Model model) {
        if (authentication == null) return "redirect:/login";

        User user = userService.findByEmail(authentication.getName());

        dashboardService.populateDashboardModel(user, model);
        return "dashboard-progress";
    }

    // Course Details (no user needed)
    @GetMapping("/student/course/{id}")
    public String courseDetails(@PathVariable Long id, Model model) {
        Course course = courseService.getCourseById(id);
        model.addAttribute("course", course);
        model.addAttribute("materials", materialService.getCourseMaterials(id));
        model.addAttribute("assignments", assignmentService.getCourseAssignments(id));
        return "student/course-detail";
    }
    
     @PostMapping("/post/create")
    public String createPost(Authentication authentication,RedirectAttributes redirectAttributes,
                             @RequestParam(required = false) String title,
                             @RequestParam String content,
                             @RequestParam String type,
                             @RequestParam(required = false) Long courseId,
                             @RequestParam(required = false) MultipartFile file,
                             @RequestParam(required = false) String linkUrl,
                             @RequestParam(required = false) String visibility) {
                            
        User user = userService.findByEmail(authentication.getName());
        List<Course> courses = courseService.getCoursesForUser(user);
        if (content.isBlank() && (file == null || file.isEmpty())) {
        redirectAttributes.addFlashAttribute("error", "Please write something or attach a file.");
        return "redirect:/student/dashboard";
    }

        try {
            postService.createPost(user, title, content, type, courseId, file, linkUrl, courses, visibility);
        } catch (IOException e) {
            log.error("File upload failed", e);
        }

        return "redirect:/student/dashboard";
    }

    // ── FEED FILTER ────────────────────────────────────────────────────────────
    // Called via JS fetch when user clicks filter buttons
    // Returns JSON list of posts filtered by type

    @GetMapping("/feed")
    @ResponseBody
    public List<Post> getFeed(Authentication authentication,
                              @RequestParam(defaultValue = "ALL") String type) {
        User user = userService.findByEmail(authentication.getName());
        return postService.getPostsForUser(user, type);
    }

    // ── LIKE / UNLIKE ──────────────────────────────────────────────────────────

    @PostMapping("/post/{id}/like")
    @ResponseBody
    public Map<String, Object> likePost(@PathVariable Long id, Authentication authentication) {
        User user = userService.findByEmail(authentication.getName());
        boolean nowLiked = postService.toggleLike(id, user);
        Post post = postService.getPostById(id);
        return Map.of("success", true, "liked", nowLiked, "count", post.getLikes());
    }

    // ── SAVE / UNSAVE ──────────────────────────────────────────────────────────

    @PostMapping("/post/{id}/save")
    @ResponseBody
    public Map<String, Object> savePost(@PathVariable Long id, Authentication authentication) {
        User user = userService.findByEmail(authentication.getName());
        boolean nowSaved = postService.toggleSave(id, user);
        Post post = postService.getPostById(id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("saved", nowSaved);

        if (nowSaved) {
            response.put("postId", post.getId());
            response.put("title", post.getTitle());
            response.put("preview", post.getContent() != null ? 
                post.getContent().substring(0, Math.min(100, post.getContent().length())) : "");
            response.put("type", post.getType());
            response.put("course", post.getCourse() != null ? post.getCourse().getCode() : "General");
            response.put("author", post.getAuthor().getFullName());
            //response.put("savedAt", LocalDateTime.now().toString());
        }

        return response;
    }

    // ── ADD COMMENT ────────────────────────────────────────────────────────────

    @PostMapping("/post/{id}/comment")
    @ResponseBody
    public Map<String, Object> addComment(@PathVariable Long id,
                                          Authentication authentication,
                                          @RequestBody Map<String, String> body) {
        User user = userService.findByEmail(authentication.getName());
        String text = body.get("text");

        if (text == null || text.isBlank()) {
            return Map.of("success", false, "error", "Comment cannot be empty");
        }

        Comment comment = postService.addComment(id, user, text);
        return Map.of(
                "success", true,
                "author", comment.getAuthor().getFullName(),
                "text", comment.getText(),
                "time", comment.getCreatedAt().toString()
        );
    }

    // ── DOWNLOAD FILE ──────────────────────────────────────────────────────────

    /*@GetMapping("/download/{attachmentId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Attachment not found"));

        try {
            Resource resource = fileStorageService.loadFileAsResource(attachment.getStoredFileName());
            String contentType = fileStorageService.getContentType(attachment.getFileType());

            // Increment download counter
            postService.incrementDownload(attachmentId);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + attachment.getFileName() + "\"")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    } */
    
    @GetMapping("/download/{attachmentId}")
    @Transactional(readOnly = true)
    public ResponseEntity<Resource> downloadFile(@PathVariable Long attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Attachment not found: " + attachmentId));

        try {
            Resource resource = fileStorageService.loadFileAsResource(attachment.getStoredFileName());

            if (!resource.exists() || !resource.isReadable()) {
                log.warn("File not found or not readable: {}", attachment.getStoredFileName());
                return ResponseEntity.notFound().build();
            }

            // Determine content type from file extension, not just stored type
            String contentType = determineContentType(attachment.getFileType(),
                                                       attachment.getFileName());

            postService.incrementDownload(attachmentId);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + attachment.getFileName() + "\"")
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                    .body(resource);

        } catch (Exception e) {
            log.error("Download failed for attachment {}: {}", attachmentId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ── Helper — robust content type detection ────────────────────────────────
    private String determineContentType(String storedFileType, String fileName) {
        // Try from stored fileType first
        if (storedFileType != null) {
            String lower = storedFileType.toLowerCase();
            // If it's already a full MIME type (e.g. "application/pdf")
            if (lower.contains("/")) return lower;
            // Otherwise treat as extension
            return extensionToMime(lower);
        }
        // Fallback: derive from original filename
        if (fileName != null && fileName.contains(".")) {
            String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
            return extensionToMime(ext);
        }
        return "application/octet-stream";
    }

    private String extensionToMime(String ext) {
        return switch (ext) {
            case "pdf"                    -> "application/pdf";
            case "doc"                    -> "application/msword";
            case "docx"                   -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "ppt"                    -> "application/vnd.ms-powerpoint";
            case "pptx"                   -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "xls"                    -> "application/vnd.ms-excel";
            case "xlsx"                   -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "zip"                    -> "application/zip";
            case "rar"                    -> "application/x-rar-compressed";
            case "jpg", "jpeg"            -> "image/jpeg";
            case "png"                    -> "image/png";
            case "gif"                    -> "image/gif";
            case "mp4"                    -> "video/mp4";
            case "mov"                    -> "video/quicktime";
            case "avi"                    -> "video/x-msvideo";
            case "txt"                    -> "text/plain";
            default                       -> "application/octet-stream";
        };
    }

    // ── VIEW FILE IN BROWSER (for images/videos) ───────────────────────────────

    @GetMapping("/view/{attachmentId}")
    public ResponseEntity<Resource> viewFile(@PathVariable Long attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Attachment not found"));

        try {
            Resource resource = fileStorageService.loadFileAsResource(attachment.getStoredFileName());
            String contentType = fileStorageService.getContentType(attachment.getFileType());

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + attachment.getFileName() + "\"")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    @GetMapping("/assignment/{id}/submit")
    @Transactional(readOnly = true)
    public String submitAssignmentPage(@PathVariable Long id, Model model,
                                        Authentication authentication) {
        String email = authentication.getName();
        User user = userService.findByEmail(email);
        Assignment assignment = assignmentService.getAssignmentById(id);

        // Check if already submitted
        boolean alreadySubmitted = assignmentService.hasStudentSubmitted(assignment, user);

        model.addAttribute("user", user);
        model.addAttribute("assignment", assignment);
        model.addAttribute("alreadySubmitted", alreadySubmitted);
        model.addAttribute("unreadMessages", messageService.countUnread(user));
        model.addAttribute("unreadNotifications", notificationService.countUnread(user));

        return "student-submit-assignment";
    }

    @PostMapping("/assignment/{id}/submit")
    public String doSubmitAssignment(@PathVariable Long id,
                                      @RequestParam(required = false) String textAnswer,
                                      @RequestParam(required = false) MultipartFile file,
                                      HttpServletRequest request,
                                      Authentication authentication,
                                      RedirectAttributes redirectAttributes) {
        String email = authentication.getName();
        User user = userService.findByEmail(email);
        Assignment assignment = assignmentService.getAssignmentById(id);

        if (assignmentService.hasStudentSubmitted(assignment, user)) {
            redirectAttributes.addFlashAttribute("error", "You have already submitted this assignment.");
            return "redirect:/student/assignments";
        }

        try {
            Submission submission = new Submission();
            submission.setAssignment(assignment);
            submission.setStudent(user);
            submission.setSubmittedAt(LocalDateTime.now());
            submission.setGraded(false);

            if (textAnswer != null && !textAnswer.isBlank()) {
                submission.setTextAnswer(textAnswer);
            }

            if (file != null && !file.isEmpty()) {
                submission.setFileName(file.getOriginalFilename());
                submission.setFileData(file.getBytes());
                submission.setFileType(file.getContentType());
                submission.setFileSize((double) file.getSize() / 1024);
            }

            assignmentService.saveSubmission(submission);

            // Auto-grade if applicable
            String subType = assignment.getSubmissionType();
            if ("QUIZ".equals(subType) || "MIXED".equals(subType)
                    || "EXACT_MATCH".equals(assignment.getAutoGradeType())
                    || "KEYWORD_MATCH".equals(assignment.getAutoGradeType())) {

                Map<Long, Long> selectedChoices = new HashMap<>();
                Map<Long, String> textAnswers = new HashMap<>();

                Enumeration<String> paramNames = request.getParameterNames();
                while (paramNames.hasMoreElements()) {
                    String param = paramNames.nextElement();
                    if (param.startsWith("question_")) {
                        Long qId = Long.parseLong(param.substring(9));
                        String val = request.getParameter(param);
                        if (val != null && !val.isBlank()) {
                            selectedChoices.put(qId, Long.parseLong(val));
                        }
                    }
                    if (param.startsWith("text_")) {
                        Long qId = Long.parseLong(param.substring(5));
                        String val = request.getParameter(param);
                        if (val != null) {
                            textAnswers.put(qId, val);
                        }
                    }
                }

                assignmentService.autoGrade(submission, selectedChoices, textAnswers);
            }

            redirectAttributes.addFlashAttribute("success", "Assignment submitted successfully!");

        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Submission failed: " + e.getMessage());
        }

        return "redirect:/student/assignments";
    }

    @GetMapping("/assignment/{id}/view")
    @Transactional(readOnly = true)
    public String viewSubmission(@PathVariable Long id, Model model,
                                  Authentication authentication) {
        String email = authentication.getName();
        User user = userService.findByEmail(email);
        Assignment assignment = assignmentService.getAssignmentById(id);

        Optional<Submission> submission = assignmentService.getSubmissionForStudent(assignment, user);

        model.addAttribute("user", user);
        model.addAttribute("assignment", assignment);
        model.addAttribute("submission", submission.orElse(null));
        model.addAttribute("unreadMessages", messageService.countUnread(user));
        model.addAttribute("unreadNotifications", notificationService.countUnread(user));

        return "student-view-submission";
    }
   @GetMapping("/messages")
    @Transactional(readOnly = true)
    public String messages(Model model, Authentication authentication) {
        User user = userService.findByEmail(authentication.getName());

        List<Course> myCourses = courseService.getCoursesForUser(user);
        List<User> coursemates = myCourses.isEmpty()
            ? new ArrayList<>()
            : courseRepository.findCoursematesInCourses(myCourses, user);

        // ✅ Add these — the template needs them
        Set<Long> onlineUserIds = messageService.getConversationsForUser(user)
            .keySet().stream()
            .filter(u -> userStatusService.isOnline(u))
            .map(User::getId)
            .collect(Collectors.toSet());

        Map<Long, Integer> unreadPerUser = new HashMap<>();
        messageService.getConversationsForUser(user).keySet().forEach(u ->
            unreadPerUser.put(u.getId(), messageService.countUnreadFromUser(u, user))
        );

        model.addAttribute("onlineUserIds", onlineUserIds);
        model.addAttribute("unreadPerUser", unreadPerUser);
        model.addAttribute("user", user);
        model.addAttribute("other", null); // ✅ explicitly null so th:if="${other != null}" works
        model.addAttribute("conversations", messageService.getConversationsForUser(user));
        model.addAttribute("unreadMessages", messageService.countUnread(user));
        model.addAttribute("unreadNotifications", notificationService.countUnread(user));
        model.addAttribute("allUsers", coursemates);
        
        model.addAttribute("allUsersExceptCurrent", 
        userService.getAllUsersExcept(user));

        return "student-messages";
    }
    
    @GetMapping("/messages/{userId}")
    @Transactional
    public String conversation(@PathVariable Long userId, Model model,
                               Authentication authentication) {
        User user = userService.findByEmail(authentication.getName());
        User other = userService.getUserById(userId);

        messageService.markConversationAsRead(user, other);

        Set<Long> onlineUserIds = messageService.getConversationsForUser(user)
            .keySet().stream()
            .filter(u -> userStatusService.isOnline(u))
            .map(User::getId)
            .collect(Collectors.toSet());

        Map<Long, Integer> unreadPerUser = new HashMap<>();
        messageService.getConversationsForUser(user).keySet().forEach(u ->
            unreadPerUser.put(u.getId(), messageService.countUnreadFromUser(u, user))
        );

        model.addAttribute("onlineUserIds", onlineUserIds);
        model.addAttribute("conversations", messageService.getConversationsForUser(user));
        model.addAttribute("unreadPerUser", unreadPerUser);
        model.addAttribute("user", user);
        model.addAttribute("other", other);
        model.addAttribute("messages", messageService.getConversation(user, other));
        model.addAttribute("unreadMessages", messageService.countUnread(user));
        model.addAttribute("unreadNotifications", notificationService.countUnread(user));
        model.addAttribute("isOtherOnline", userStatusService.isOnline(other)); // ✅ fixes line 898
        model.addAttribute("allUsersExceptCurrent", 
        userService.getAllUsersExcept(user));
        return "student-conversation";
    }
    
    @PostMapping("/messages/send")
    public String sendMessage(Authentication authentication,
                              @RequestParam Long recipientId,
                              @RequestParam String content,
                              @RequestParam(required = false, defaultValue = "Message") String subject,
                              RedirectAttributes redirectAttributes) {
        User user = userService.findByEmail(authentication.getName());
        User recipient = userService.getUserById(recipientId);
        messageService.sendMessage(user, recipient, content, subject);
        redirectAttributes.addFlashAttribute("success", "Message sent!");
        return "redirect:/student/messages/" + recipientId;
    }
    @GetMapping("/messages/{id}/refresh")
    @ResponseBody
    public String refresh(@PathVariable Long id, Authentication auth, Model model) {
        User user = userService.findByEmail(auth.getName());
        User other = userService.getUserById(id);

        model.addAttribute("messages", messageService.getConversation(user, other));

        return "fragments/message-list :: messages";
    }

    @GetMapping("/notifications")
    @Transactional
    public String notifications(Model model, Authentication authentication) {
        User user = userService.findByEmail(authentication.getName());
        model.addAttribute("user", user);
        model.addAttribute("notifications", notificationService.getNotificationsForUser(user));
        model.addAttribute("unreadMessages", messageService.countUnread(user));
        model.addAttribute("unreadNotifications", notificationService.countUnread(user));
        notificationService.markAllAsRead(user);
        return "student-notifications";
    }

    @PostMapping("/notifications/mark-read")
    @ResponseBody
    public Map<String, Object> markNotificationsRead(Authentication authentication) {
        User user = userService.findByEmail(authentication.getName());
        notificationService.markAllAsRead(user);
        return Map.of("success", true);
    }
    // ═══════════════ COURSE ENROLLMENT ═══════════════

    @GetMapping("/courses/enroll")
    @Transactional(readOnly = true)
    public String enrollCourses(Authentication authentication, Model model) {
        String email = authentication.getName();
        User user = userService.findByEmail(email);

        List<Course> allCourses = courseService.getAllCourses();
        List<Course> enrolledCourses = courseService.getCoursesForUser(user);
        Set<Long> enrolledIds = enrolledCourses.stream().map(Course::getId).collect(Collectors.toSet());
        List<Course> availableCourses = allCourses.stream()
            .filter(c -> !enrolledIds.contains(c.getId()))
            .collect(Collectors.toList());

        // Initialize lazy collections within the transaction to avoid
        // PostgreSQL Large Object auto-commit issue with @Lob profile pictures
        for (Course course : allCourses) {
            Hibernate.initialize(course.getStudents());
            if (course.getLecturer() != null) {
                Hibernate.initialize(course.getLecturer());
            }
        }

        model.addAttribute("user", user);
        model.addAttribute("availableCourses", availableCourses);
        model.addAttribute("enrolledCourses", enrolledCourses);
        model.addAttribute("unreadMessages", messageService.countUnread(user));
        model.addAttribute("unreadNotifications", notificationService.countUnread(user));
        model.addAttribute("pendingAssignments", assignmentService.countPending(user));
        model.addAttribute("savedItems", savedService != null ? savedService.countSaved(user) : 0);

        return "student-enroll-courses";
    }

    @PostMapping("/courses/{id}/enroll")
    @Transactional
    public String enrollCourse(@PathVariable Long id,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        String email = authentication.getName();
        User user = userService.findByEmail(email);
        Course course = courseService.getCourseById(id);

        if (course == null) {
            redirectAttributes.addFlashAttribute("error", "Course not found.");
            return "redirect:/student/courses/enroll";
        }

        if (user.getCourses() != null && user.getCourses().contains(course)) {
            redirectAttributes.addFlashAttribute("error", "You are already enrolled in this course.");
            return "redirect:/student/courses/enroll";
        }

        if (user.getCourses() == null) {
            user.setCourses(new ArrayList<>());
        }
        user.getCourses().add(course);
        course.getStudents().add(user);
        userService.saveUser(user);

        redirectAttributes.addFlashAttribute("success", "Successfully enrolled in " + course.getName());
        return "redirect:/student/courses/enroll";
    }

    @PostMapping("/courses/{id}/drop")
    @Transactional
    public String dropCourse(@PathVariable Long id,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        String email = authentication.getName();
        User user = userService.findByEmail(email);
        Course course = courseService.getCourseById(id);

        if (course == null) {
            redirectAttributes.addFlashAttribute("error", "Course not found.");
            return "redirect:/student/courses/enroll";
        }

        if (user.getCourses() != null) {
            user.getCourses().remove(course);
            course.getStudents().remove(user);
            userService.saveUser(user);
        }

        redirectAttributes.addFlashAttribute("success", "Successfully dropped " + course.getName());
        return "redirect:/student/courses/enroll";
    }

    @GetMapping("/badges")
    @ResponseBody
    @Transactional(readOnly = true)
    public Map<String, Object> getBadges(Authentication authentication) {
        User user = userService.findByEmail(authentication.getName());
        return Map.of(
            "unreadMessages",      messageService.countUnread(user),
            "unreadNotifications", notificationService.countUnread(user),
            "pendingAssignments",  assignmentService.countPending(user)
        );
    }
    @GetMapping("/stats")
    @ResponseBody
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardStats(Authentication authentication) {
        User user = userService.findByEmail(authentication.getName());
        List<Course> courses = courseService.getCoursesForUser(user);

        List<Assignment> all = courses.stream()
            .flatMap(c -> assignmentService.getCourseAssignments(c.getId()).stream())
            .collect(Collectors.toList());

        long pending   = all.stream().filter(a -> !assignmentService.hasStudentSubmitted(a, user)).count();
        long submitted = all.stream().filter(a ->  assignmentService.hasStudentSubmitted(a, user)).count();
        long graded    = all.stream().filter(a ->  assignmentService.isGradedForStudent(a, user)).count();

        return Map.of(
            "totalAssignments",  all.size(),
            "pending",           pending,
            "submitted",         submitted,
            "graded",            graded,
            "totalMaterials",    materialService.countMaterialsForUser(user),
            "totalCourses",      courses.size()
        );
    }

    // ═══════════════ QUIZ ENGINE ═══════════════

    @GetMapping("/quizzes")
    @Transactional
    public String quizzes(Model model, Authentication authentication) {
        User student = userService.findByEmail(authentication.getName());
        List<Course> courses = student.getCourses();
        List<Quiz> quizzes = new ArrayList<>();
        Map<Long, Integer> attemptsCount = new HashMap<>();
        Map<Long, QuizAttempt> lastAttempts = new HashMap<>();

        for (Course course : courses) {
            List<Quiz> courseQuizzes = quizService.getQuizzesForCourse(course);
            for (Quiz quiz : courseQuizzes) {
                quizzes.add(quiz);
                int count = quizAttemptService.getAttemptCount(quiz, student);
                attemptsCount.put(quiz.getId(), count);
                List<QuizAttempt> attempts = quizAttemptService.getAttemptsForStudent(quiz, student);
                if (!attempts.isEmpty()) {
                    lastAttempts.put(quiz.getId(), attempts.get(0));
                }
            }
        }

        model.addAttribute("user", student);
        model.addAttribute("quizzes", quizzes);
        model.addAttribute("attemptsCount", attemptsCount);
        model.addAttribute("lastAttempts", lastAttempts);
        model.addAttribute("courses", courses);
        model.addAttribute("unreadMessages", messageService.countUnread(student));
        model.addAttribute("unreadNotifications", notificationService.countUnread(student));
        return "student-quizzes";
    }

    @GetMapping("/quizzes/{id}/start")
    @Transactional
    public String startQuiz(@PathVariable Long id, Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        User student = userService.findByEmail(authentication.getName());
        Quiz quiz = quizService.getQuizById(id);

        QuizAttempt inProgress = quizAttemptService.getInProgressAttempt(quiz, student);
        if (inProgress != null) {
            return "redirect:/student/quizzes/" + quiz.getId() + "/take/" + inProgress.getId();
        }

        if (quiz.getMaxAttempts() != null) {
            int count = quizAttemptService.getAttemptCount(quiz, student);
            if (count >= quiz.getMaxAttempts()) {
                redirectAttributes.addFlashAttribute("error", "Maximum attempts reached");
                return "redirect:/student/quizzes";
            }
        }

        if (quiz.getDueDate() != null && quiz.getDueDate().isBefore(LocalDateTime.now())) {
            redirectAttributes.addFlashAttribute("error", "This quiz is past due date");
            return "redirect:/student/quizzes";
        }

        QuizAttempt attempt = quizAttemptService.startAttempt(quiz, student);
        return "redirect:/student/quizzes/" + quiz.getId() + "/take/" + attempt.getId();
    }

    @GetMapping("/quizzes/{id}/take/{attemptId}")
    @Transactional
    public String takeQuiz(@PathVariable Long id, @PathVariable Long attemptId,
                            Model model, Authentication authentication) {
        QuizAttempt attempt = quizAttemptService.getAttemptById(attemptId);
        Quiz quiz = attempt.getQuiz();

        if (!"IN_PROGRESS".equals(attempt.getStatus())) {
            return "redirect:/student/quizzes/" + id + "/result/" + attemptId;
        }

        model.addAttribute("user", attempt.getStudent());
        model.addAttribute("quiz", quiz);
        model.addAttribute("attempt", attempt);
        model.addAttribute("unreadMessages", messageService.countUnread(attempt.getStudent()));
        model.addAttribute("unreadNotifications", notificationService.countUnread(attempt.getStudent()));
        return "student-take-quiz";
    }

    @PostMapping("/quizzes/{id}/submit/{attemptId}")
    @Transactional
    public String submitQuiz(@PathVariable Long id, @PathVariable Long attemptId,
                              HttpServletRequest request,
                              RedirectAttributes redirectAttributes) {
        QuizAttempt attempt = quizAttemptService.getAttemptById(attemptId);

        if (!"IN_PROGRESS".equals(attempt.getStatus())) {
            return "redirect:/student/quizzes/" + id + "/result/" + attemptId;
        }

        if (quizAttemptService.isTimeExpired(attempt)) {
            redirectAttributes.addFlashAttribute("error", "Time limit exceeded");
            quizAttemptService.submitAttempt(attemptId, Map.of(), Map.of());
            return "redirect:/student/quizzes/" + id + "/result/" + attemptId;
        }

        Map<Long, Long> selectedChoices = new HashMap<>();
        Map<Long, String> textAnswers = new HashMap<>();

        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String param = paramNames.nextElement();
            if (param.startsWith("question_")) {
                Long questionId = Long.parseLong(param.substring(9));
                String value = request.getParameter(param);
                if (value != null && !value.isBlank()) {
                    selectedChoices.put(questionId, Long.parseLong(value));
                }
            }
            if (param.startsWith("text_")) {
                Long questionId = Long.parseLong(param.substring(5));
                String value = request.getParameter(param);
                if (value != null && !value.isBlank()) {
                    textAnswers.put(questionId, value);
                }
            }
        }

        quizAttemptService.submitAttempt(attemptId, selectedChoices, textAnswers);

        return "redirect:/student/quizzes/" + id + "/result/" + attemptId;
    }

    @GetMapping("/quizzes/{id}/result/{attemptId}")
    public String quizResult(@PathVariable Long id, @PathVariable Long attemptId,
                              Model model, Authentication authentication) {
        QuizAttempt attempt = quizAttemptService.getAttemptById(attemptId);
        Quiz quiz = attempt.getQuiz();

        model.addAttribute("user", attempt.getStudent());
        model.addAttribute("quiz", quiz);
        model.addAttribute("attempt", attempt);
        model.addAttribute("unreadMessages", messageService.countUnread(attempt.getStudent()));
        model.addAttribute("unreadNotifications", notificationService.countUnread(attempt.getStudent()));
        return "student-quiz-result";
    }

    // ── STUDENT PROFILE ──────────────────────────────────────────────────────────

    @GetMapping("/profile")
    public String profile(Model model, Authentication authentication) {
        User student = userService.findByEmail(authentication.getName());

        model.addAttribute("user", student);
        model.addAttribute("unreadMessages", messageService.countUnread(student));
        model.addAttribute("unreadNotifications", notificationService.countUnread(student));
        model.addAttribute("courses", student.getCourses());

        return "student-profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(Authentication authentication,
                                @RequestParam String fullName,
                                @RequestParam(required = false) String phoneNumber,
                                @RequestParam(required = false) String bio,
                                @RequestParam(required = false) MultipartFile profilePicture,
                                RedirectAttributes redirectAttributes) {
        User student = userService.findByEmail(authentication.getName());

        student.setFullName(fullName);
        student.setPhoneNumber(phoneNumber);
        student.setBio(bio);

        if (profilePicture != null && !profilePicture.isEmpty()) {
            try {
                student.setProfilePicture(profilePicture.getBytes());
            } catch (IOException e) {
                redirectAttributes.addFlashAttribute("error", "Failed to upload profile picture");
            }
        }

        userService.saveUser(student);
        redirectAttributes.addFlashAttribute("success", "Profile updated successfully");

        return "redirect:/student/profile";
    }
    
}