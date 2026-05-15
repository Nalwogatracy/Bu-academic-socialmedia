package com.finalyearproject.controller;

import com.finalyearproject.model.*;
import com.finalyearproject.service.*;
import com.finalyearproject.repository.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/lecturer")
public class LecturerController {

    private final UserService         userService;
    private final CourseService       courseService;
    private final PostService         postService;
    private final AssignmentService   assignmentService;
    private final MaterialService     materialService;
    private final NotificationService notificationService;
    private final MessageService      messageService;
    private final FileStorageService    fileStorageService;
    private final PostRepository        postRepository;
    private final UserStatusService     userStatusService;
    private final CourseRepository      courseRepository;
    private final SseController         sseController;
    @Autowired
    private SubmissionRepository submissionRepository;
    @Autowired 
    private MaterialRepository materialRepository;
    @Autowired
    private CourseEnrollmentRepository enrollmentRepository;
    @Autowired 
    private AttachmentRepository attachmentRepository;

    public LecturerController(UserService userService,
                               CourseService courseService,
                               PostService postService,
                               AssignmentService assignmentService,
                               MaterialService materialService,
                               NotificationService notificationService,
                               MessageService messageService,
                               FileStorageService fileStorageService,
                               PostRepository postRepository,
                               UserStatusService userStatusService,
                               CourseRepository courseRepository,
                               SseController sseController) {
        this.userService         = userService;
        this.courseService       = courseService;
        this.postService         = postService;
        this.assignmentService   = assignmentService;
        this.materialService     = materialService;
        this.notificationService = notificationService;
        this.messageService      = messageService;
        this.fileStorageService  = fileStorageService;
        this.postRepository      = postRepository;
        this.userStatusService   = userStatusService;
        this.courseRepository    = courseRepository;
        this.sseController       = sseController;
    }

    @GetMapping("/dashboard")
    @Transactional(readOnly = true)
    public String dashboard(Model model, Authentication authentication,Long id) {

        User lecturer = userService.findByEmail(authentication.getName());
        model.addAttribute("user", lecturer);
        model.addAttribute("unreadMessages",      messageService.countUnread(lecturer));
        model.addAttribute("unreadNotifications", notificationService.countUnread(lecturer));

        List<Course> courses = courseService.getCoursesForLecturer(lecturer);
        model.addAttribute("courses", courses);
        
        //model.addAttribute("materialsList", new ArrayList<>(course.getMaterials()));
       /* if (id != null) {
                Course course = courseService.getCourseById(id);
                model.addAttribute("materialsList", new ArrayList<>(course.getMaterials()));
            } else {
                // Otherwise, combine all materials
                List<Material> allMaterials = courses.stream()
                        .filter(c -> c.getMaterials() != null)
                        .flatMap(c -> c.getMaterials().stream())
                        .collect(Collectors.toList());
                model.addAttribute("materialsList", allMaterials);
            } */
        //Course course = courseService.getCourseById(id);
        //model.addAttribute("materialsList", new ArrayList<>(course.getMaterials()));
        if (id != null) {
            Course course = courseService.getCourseById(id);
            model.addAttribute("materialsList", new ArrayList<>(course.getMaterials()));
        } else {
            // Combine all materials from all courses
            List<Material> allMaterials = courses.stream()
                    .filter(c -> c.getMaterials() != null)
                    .flatMap(c -> c.getMaterials().stream())
                    .collect(Collectors.toList());
            model.addAttribute("materialsList", allMaterials);
        }

// Remove the extra call to getCourseById(id)

        int activeCourses = courses.size();

        // Course.students is Set<User> — .size() works on Set fine
        int totalStudents = courses.stream()
                .mapToInt(c -> c.getStudents() != null ? c.getStudents().size() : 0)
                .sum();

        long materialsShared = materialService.countMaterialsByLecturer(lecturer);
        int  avgEngagement   = calcAvgEngagement(courses);

        model.addAttribute("activeCourses",   activeCourses);
        model.addAttribute("totalStudents",   totalStudents);
        model.addAttribute("materialsShared", materialsShared);
        model.addAttribute("avgEngagement",   avgEngagement);

        long pendingGrades = assignmentService.countPendingGradesForLecturer(lecturer);
        int  newQuestions  = postRepository.countUnansweredDiscussionsForLecturer(lecturer);
        model.addAttribute("pendingGrades", pendingGrades);
        model.addAttribute("newQuestions",  newQuestions);

        model.addAttribute("recentActivity", buildRecentActivity(lecturer, courses));
        model.addAttribute("pendingReviews", assignmentService.getPendingReviewsForLecturer(lecturer));

        model.addAttribute("chartLabels",      List.of("Mon","Tue","Wed","Thu","Fri","Sat","Sun"));
        model.addAttribute("chartDatasets",    buildEngagementDatasets(courses, "week"));
        model.addAttribute("submissionLabels", courses.stream().map(Course::getCode).collect(Collectors.toList()));
        model.addAttribute("submittedData",    courses.stream().map(c -> assignmentService.countSubmittedForCourse(c)).collect(Collectors.toList()));
        model.addAttribute("pendingData",      courses.stream().map(c -> assignmentService.countPendingForCourse(c)).collect(Collectors.toList()));
        
        long materialCount = materialRepository.countByCourses(courses);
        model.addAttribute("materialCount", materialCount);

        return "lecturer-dashboard";
    }

    @PostMapping("/post/create")
    public String createPost(Authentication authentication,
                             @RequestParam(required = false) Long          courseId,
                             @RequestParam                   String        postType,
                             @RequestParam                   String        title,
                             @RequestParam(required = false) String        description,
                             @RequestParam(required = false) MultipartFile file,
                             @RequestParam(required = false) String        visibility,
                             @RequestParam(required = false)
                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                             LocalDateTime dueDate,
                             @RequestParam(required = false, defaultValue = "false") boolean notifyEmail,
                             @RequestParam(required = false, defaultValue = "false") boolean allowComments,
                             @RequestParam(required = false, defaultValue = "false") boolean pinToTop) {
        

        User         lecturer = userService.findByEmail(authentication.getName());
        List<Course> courses  = courseService.getCoursesForLecturer(lecturer);

        try {
            postService.createPost(
                    lecturer, title,
                    description != null ? description : "",
                    postType.toUpperCase(),
                    courseId,
                    (file != null && !file.isEmpty()) ? file : null,
                    null,
                    courses,
                    visibility != null ? visibility : "COURSE"
            );
        } catch (IOException e) {
            System.err.println("File upload error: " + e.getMessage());
        }
        return "redirect:/lecturer/dashboard";
    }

    @PostMapping("/assignment/{id}/grade")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> gradeAssignment(
            @PathVariable Long id,
            @RequestBody  Map<String, Object> body,
            Authentication authentication) {
        User lecturer = userService.findByEmail(authentication.getName());
        try {
            int    score    = Integer.parseInt(body.get("score").toString());
            String feedback = body.getOrDefault("feedback", "").toString();
            assignmentService.gradeSubmission(id, score, feedback, lecturer);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/charts/engagement")
    @ResponseBody
    public Map<String, Object> engagementChartData(
            @RequestParam(defaultValue = "week") String period,
            Authentication authentication) {
        User         lecturer = userService.findByEmail(authentication.getName());
        List<Course> courses  = courseService.getCoursesForLecturer(lecturer);
        return Map.of("labels", labelsForPeriod(period), "datasets", buildEngagementDatasets(courses, period));
    }

    @GetMapping("/charts/submissions")
    @ResponseBody
    public Map<String, Object> submissionsChartData(Authentication authentication) {
        User         lecturer = userService.findByEmail(authentication.getName());
        List<Course> courses  = courseService.getCoursesForLecturer(lecturer);
        return Map.of(
                "labels",    courses.stream().map(Course::getCode).collect(Collectors.toList()),
                "submitted", courses.stream().map(c -> assignmentService.countSubmittedForCourse(c)).collect(Collectors.toList()),
                "pending",   courses.stream().map(c -> assignmentService.countPendingForCourse(c)).collect(Collectors.toList())
        );
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private List<Map<String, Object>> buildRecentActivity(User lecturer, List<Course> courses) {
        List<Map<String, Object>> activities = new ArrayList<>();
        postRepository.findByAuthorOrderByCreatedAtDesc(lecturer).stream().limit(2)
                .forEach(p -> activities.add(Map.of(
                        "icon", iconForType(p.getType()), "text", "Uploaded: " + p.getTitle(),
                        "time", timeAgo(p.getCreatedAt()), "status", "new")));
        assignmentService.getRecentSubmissionsForLecturer(lecturer, 2)
                .forEach(s -> activities.add(Map.of(
                        "icon", "fas fa-user-graduate",
                        "text", s.get("studentName") + " submitted " + s.get("assignmentTitle"),
                        "time", s.get("timeAgo"), "status", "")));
        int unanswered = postRepository.countUnansweredDiscussionsForLecturer(lecturer);
        if (unanswered > 0) activities.add(Map.of(
                "icon", "fas fa-question-circle",
                "text", unanswered + " new question(s) in Discussion Forum",
                "time", "Today", "status", "urgent"));
        return activities.stream().limit(6).collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildEngagementDatasets(List<Course> courses, String period) {
        String[] colors = {"#667eea", "#764ba2", "#f093fb", "#f5576c", "#4facfe"};
        List<Map<String, Object>> datasets = new ArrayList<>();
        int i = 0;
        for (Course c : courses.stream().limit(5).toList()) {
            Map<String, Object> ds = new LinkedHashMap<>();
            ds.put("label",       c.getCode());
            ds.put("data",        randomEngagementData(c, period));
            ds.put("borderColor", colors[i % colors.length]);
            ds.put("tension",     0.4);
            datasets.add(ds); i++;
        }
        return datasets;
    }

    private List<Integer> randomEngagementData(Course course, String period) {
        int points = period.equals("month") ? 4 : period.equals("semester") ? 6 : 7;
        Random rng = new Random(course.getId());
        List<Integer> data = new ArrayList<>();
        for (int i = 0; i < points; i++) data.add(25 + rng.nextInt(70));
        return data;
    }

    private List<String> labelsForPeriod(String period) {
        return switch (period) {
            case "month"    -> List.of("Week 1","Week 2","Week 3","Week 4");
            case "semester" -> List.of("Jan","Feb","Mar","Apr","May","Jun");
            default         -> List.of("Mon","Tue","Wed","Thu","Fri","Sat","Sun");
        };
    }

    /**
     * Course.students is Set<User> — iterate it directly, no List cast needed.
     */
    private int calcAvgEngagement(List<Course> courses) {
        if (courses.isEmpty()) return 0;
        return courses.stream()
                .mapToInt(c -> {
                    Set<User> students = c.getStudents(); // ← Set<User> correct
                    if (students == null || students.isEmpty()) return 0;
                    return (int) students.stream()
                            .mapToDouble(s -> userService.calculateEngagement(s))
                            .average().orElse(0);
                }).sum() / courses.size();
    }

    private String timeAgo(LocalDateTime dt) {
        if (dt == null) return "Unknown";
        long mins = Duration.between(dt, LocalDateTime.now()).toMinutes();
        if (mins < 1)    return "Just now";
        if (mins < 60)   return mins + " min ago";
        if (mins < 1440) return (mins / 60) + " hours ago";
        if (mins < 2880) return "Yesterday";
        return (mins / 1440) + " days ago";
    }

    private String iconForType(String type) {
        if (type == null) return "fas fa-newspaper";
        return switch (type.toUpperCase()) {
            case "MATERIAL"     -> "fas fa-file-upload";
            case "ASSIGNMENT"   -> "fas fa-tasks";
            case "ANNOUNCEMENT" -> "fas fa-bullhorn";
            case "DISCUSSION"   -> "fas fa-comments";
            default             -> "fas fa-newspaper";
        };
    }
        @GetMapping("/courses")
        @Transactional(readOnly = true)
    public String courses(Model model, Authentication authentication) {
        User lecturer = userService.findByEmail(authentication.getName());
        List<Course> courses = courseService.getCoursesForLecturer(lecturer);
        
        // Enhance courses with additional data
        courses.forEach(c -> {
            long materialsCount = materialService.countMaterialsByCourse(c.getId());
            c.setMaterialsCount((int) materialsCount);
        });
        
        model.addAttribute("user", lecturer);
        model.addAttribute("courses", courses);
        model.addAttribute("totalStudents", courses.stream().mapToInt(c -> c.getStudents().size()).sum());
        model.addAttribute("unreadMessages", messageService.countUnread(lecturer));
        model.addAttribute("unreadNotifications", notificationService.countUnread(lecturer));
        return "lecturer-courses";
    }
    
    @GetMapping("/course/{id}")
    @Transactional(readOnly = true)
    public String courseDetails(@PathVariable Long id, Model model, Authentication authentication) {
        User lecturer = userService.findByEmail(authentication.getName());
        Course course = courseService.getCourseById(id);
        
        // Verify lecturer owns this course
        if (!course.getLecturer().getId().equals(lecturer.getId())) {
            return "redirect:/lecturer/courses?error=unauthorized";
        }
        List<Course> courses = courseService.getCoursesForLecturer(lecturer);
        
        model.addAttribute("user", lecturer);
        model.addAttribute("course", course);
        model.addAttribute("courses", courses);
        model.addAttribute("materials", materialService.getMaterialsForCourses(List.of(course)));
        model.addAttribute("assignments", assignmentService.getCourseAssignments(course.getId()));
        model.addAttribute("students", course.getStudents());
        model.addAttribute("recentActivities", buildCourseRecentActivity(course));
        model.addAttribute("unreadMessages", messageService.countUnread(lecturer));
        model.addAttribute("unreadNotifications", notificationService.countUnread(lecturer));
        
              return "lecturer-course-details";
    }
    @GetMapping("/materials")
    @Transactional(readOnly = true)
    public String materials(Model model, Authentication authentication) {
        User lecturer = userService.findByEmail(authentication.getName());
        List<Material> materials = materialService.getMaterialsForCourses(courseService.getCoursesForLecturer(lecturer));
        List<Course> courses = courseService.getCoursesForLecturer(lecturer);
        
        model.addAttribute("user", lecturer);
        model.addAttribute("materials", materials);
        model.addAttribute("courses", courses);
        model.addAttribute("totalMaterials", materials.size());
        model.addAttribute("totalDownloads", materials.stream().mapToInt(m -> 0).sum()); // You'll need to implement download count
        model.addAttribute("storageUsed", calculateStorageUsed(materials));
        model.addAttribute("unreadMessages", messageService.countUnread(lecturer));
        model.addAttribute("unreadNotifications", notificationService.countUnread(lecturer));
        
        return "lecturer-materials";
    }
    
    @PostMapping("/materials/upload")
    public String uploadMaterial(
            @RequestParam("courseId") Long courseId,
            @RequestParam("title") String title,
            @RequestParam("type") String type,
            @RequestParam("visibility") String visibility,
            @RequestParam("description") String description,
            @RequestParam("file") MultipartFile file,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        User lecturer = userService.findByEmail(authentication.getName());

        try {
            Material material = materialService.saveMaterial(
                    courseId,
                    title,
                    type,
                    visibility,
                    description,
                    file,
                    lecturer
            );
            Course course = courseService.getCourseById(courseId);
            Post feedPost = new Post();
            feedPost.setTitle(title);
            feedPost.setContent(description != null ? description : "New material uploaded.");
            feedPost.setType("MATERIAL");
            feedPost.setAuthor(lecturer);
            feedPost.setCourse(course);
            feedPost.setCreatedAt(LocalDateTime.now());
            feedPost.setVisibility(visibility != null ? visibility : "COURSE");
            Post savedPost = postRepository.save(feedPost);
            
            if (file != null && !file.isEmpty()) {
            Attachment attachment = fileStorageService.storeFile(file);
            attachment.setPost(savedPost);

            String originalName = file.getOriginalFilename();
            if (originalName != null && originalName.contains(".")) {
                attachment.setFileType(
                    originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase()
                );
            }

            System.out.println(">>> SAVING ATTACHMENT: " + originalName
                + " | type=" + attachment.getFileType());

            attachmentRepository.save(attachment);
            System.out.println(">>> ATTACHMENT SAVED OK");
        }

        //redirectAttributes.addFlashAttribute("success", "Material uploaded successfully!");

            redirectAttributes.addFlashAttribute("success", "Material uploaded successfully!");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Failed to upload material: " + e.getMessage());
        }

        return "redirect:/lecturer/materials";
    }

    @GetMapping("/material/{id}/download")
    public ResponseEntity<byte[]> downloadMaterial(@PathVariable Long id) {
        Material material = materialService.getMaterialById(id);
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(material.getFileType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + material.getFileName() + "\"")
                .body(material.getFileData());
    }

    @GetMapping("/material/{id}/preview")
    public ResponseEntity<byte[]> previewMaterial(@PathVariable Long id) {
        Material material = materialService.getMaterialById(id);
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(material.getFileType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + material.getFileName() + "\"")
                .body(material.getFileData());
    }
        
        @PostMapping("/material/{id}/delete")
        public String deleteMaterial(@PathVariable Long id, Authentication authentication) {
            User lecturer = userService.findByEmail(authentication.getName());
            Material material = materialService.getMaterialById(id);

            // Verify ownership
            if (material.getLecturer().getId().equals(lecturer.getId())) {
                materialService.deleteMaterial(id);
            }

            return "redirect:/lecturer/materials";
        }
          @GetMapping("/material/{id}/edit")
        public String editMaterialForm(@PathVariable Long id, Model model, Authentication authentication) {
            User lecturer = userService.findByEmail(authentication.getName());
            Material material = materialService.getMaterialById(id);

            if (!material.getLecturer().getId().equals(lecturer.getId())) {
                return "redirect:/lecturer/materials";
            }

            model.addAttribute("user", lecturer);
            model.addAttribute("material", material);
            model.addAttribute("courses", courseService.getCoursesForLecturer(lecturer));

            return "lecturer-material-edit";
        }
             
         @PostMapping("/material/{id}/update")
    public String updateMaterial(@PathVariable Long id,
                                 @RequestParam String title,
                                 @RequestParam String description,
                                 @RequestParam Long courseId,
                                 @RequestParam String type,
                                 @RequestParam String visibility,
                                 RedirectAttributes redirectAttributes) {
        Material material = materialService.getMaterialById(id);
        material.setTitle(title);
        material.setDescription(description);
        material.setCourse(courseService.getCourseById(courseId));
        
       materialService.updateMaterial(id, title, type, visibility, description);
        redirectAttributes.addFlashAttribute("success", "Material updated successfully");
        
        return "redirect:/lecturer/materials";
    }
    /* @GetMapping("/assignments")
    public String assignments(Model model, Authentication authentication) {

        User lecturer = userService.findByEmail(authentication.getName());

        List<Course> courses = courseService.getCoursesForLecturer(lecturer);

        List<Assignment> assignments = courses.stream()
            .flatMap(course -> assignmentService.getCourseAssignments(course.getId()).stream())
            .collect(Collectors.toList());

        // ✅ Step 1: Get course IDs
        List<Long> courseIds = courses.stream()
            .map(Course::getId)
            .collect(Collectors.toList());

        // ✅ Step 2: Fetch counts (YOU MISSED THIS)
        List<Object[]> counts = enrollmentRepository.countStudentsByCourses(courseIds);

        // ✅ Step 3: Convert to map
        Map<Long, Integer> studentCountMap = new HashMap<>();

        for (Object[] row : counts) {
            Long courseId = (Long) row[0];
            Long count = (Long) row[1];
            studentCountMap.put(courseId, count.intValue());
        }

        // ✅ Step 4: Set transient field
        for (Assignment assignment : assignments) {
            int count = studentCountMap.getOrDefault(
                assignment.getCourse().getId(), 0
            );
            assignment.setTotalStudents(count);
        }
        for (Assignment assignment : assignments) {
            int pending = assignmentService.hasPendingGrading(assignment) ? 1 : 0; // or count logic
            assignment.setPendingGrading(pending);
        }

        // Categorize assignments
        List<Assignment> activeAssignments = assignments.stream()
            .filter(a -> a.getDueDate().isAfter(LocalDateTime.now()))
            .collect(Collectors.toList());

        List<Assignment> pendingGrading = assignments.stream()
            .filter(a -> assignmentService.hasPendingGrading(a))
            .collect(Collectors.toList());

        List<Assignment> pastDueAssignments = assignments.stream()
            .filter(a -> a.getDueDate().isBefore(LocalDateTime.now()))
            .collect(Collectors.toList());

        model.addAttribute("user", lecturer);
        model.addAttribute("assignments", assignments);
        model.addAttribute("activeAssignments", activeAssignments.size());
        model.addAttribute("pendingGrades", pendingGrading.size());
        model.addAttribute("pastDueAssignments", pastDueAssignments.size());
        model.addAttribute("totalAssignments", assignments.size());
        model.addAttribute("courses", courses);
        model.addAttribute("unreadMessages", messageService.countUnread(lecturer));
        model.addAttribute("unreadNotifications", notificationService.countUnread(lecturer));

        return "lecturer-assignments";
    } */
    
    @GetMapping("/assignments")
    public String assignments(Model model, Authentication authentication,
                              @RequestParam(defaultValue = "all") String filter) {

        User lecturer = userService.findByEmail(authentication.getName());
        List<Course> courses = courseService.getCoursesForLecturer(lecturer);

        List<Assignment> allAssignments = courses.stream()
            .flatMap(course -> assignmentService.getCourseAssignments(course.getId()).stream())
            .collect(Collectors.toList());

        // Set transient fields
        List<Long> courseIds = courses.stream().map(Course::getId).collect(Collectors.toList());
        List<Object[]> counts = enrollmentRepository.countStudentsByCourses(courseIds);
        Map<Long, Integer> studentCountMap = new HashMap<>();
        for (Object[] row : counts) {
            studentCountMap.put((Long) row[0], ((Long) row[1]).intValue());
        }
        for (Assignment a : allAssignments) {
            a.setTotalStudents(studentCountMap.getOrDefault(a.getCourse().getId(), 0));
            a.setPendingGrading(assignmentService.hasPendingGrading(a) ? 1 : 0);
        }

        // Categorize for tab counts (always use full list)
        List<Assignment> activeList = allAssignments.stream()
            .filter(a -> a.getDueDate().isAfter(LocalDateTime.now()))
            .collect(Collectors.toList());
        List<Assignment> pendingList = allAssignments.stream()
            .filter(a -> assignmentService.hasPendingGrading(a))
            .collect(Collectors.toList());
        List<Assignment> pastList = allAssignments.stream()
            .filter(a -> a.getDueDate().isBefore(LocalDateTime.now()))
            .collect(Collectors.toList());

        // Apply filter for display
        List<Assignment> displayAssignments = switch (filter) {
            case "active"  -> activeList;
            case "pending" -> pendingList;
            case "past"    -> pastList;
            default        -> allAssignments;
        };

        model.addAttribute("user", lecturer);
        model.addAttribute("assignments", displayAssignments);  // filtered list
        model.addAttribute("activeFilter", filter);
        model.addAttribute("totalAssignments", allAssignments.size());
        model.addAttribute("activeAssignments", activeList.size());
        model.addAttribute("pendingGrades", pendingList.size());
        model.addAttribute("pastDueAssignments", pastList.size());
        model.addAttribute("courses", courses);
        model.addAttribute("unreadMessages", messageService.countUnread(lecturer));
        model.addAttribute("unreadNotifications", notificationService.countUnread(lecturer));

        return "lecturer-assignments";
    }


    @PostMapping("/assignment/create")
    public String createAssignment(Authentication authentication,
                                   @RequestParam Long courseId,
                                   @RequestParam String title,
                                   @RequestParam String description,
                                   @RequestParam String dueDate,
                                   @RequestParam Integer totalPoints,
                                   @RequestParam(required = false) MultipartFile file,
                                   @RequestParam String visibility,
                                   RedirectAttributes redirectAttributes) {
        User lecturer = userService.findByEmail(authentication.getName());
        Course course = courseService.getCourseById(courseId);
        
         LocalDateTime parsedDueDate;
            try {
                parsedDueDate = LocalDate.parse(dueDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                                         .atTime(23, 59, 59);
            } catch (DateTimeParseException e) {
                redirectAttributes.addFlashAttribute("error", "Invalid date: " + dueDate);
                return "redirect:/lecturer/assignments";
            }

        Assignment assignment = new Assignment();
        assignment.setTitle(title);
        assignment.setDescription(description);
        assignment.setDueDate(parsedDueDate);
        assignment.setTotalPoints(totalPoints);
        assignment.setCourse(course);
        assignment.setLecturer(lecturer);
        assignment.setCreatedAt(LocalDateTime.now());
        assignment.setVisibility(visibility);
        
        assignmentService.saveAssignment(assignment);

        // ✅ Always create feed post — outside the file block
        Post feedPost = new Post();
        feedPost.setTitle("📋 New Assignment: " + title);
        feedPost.setContent(description + "\n\nDue: " +
                parsedDueDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) +
                " at 11:59 PM | Points: " + totalPoints);
        feedPost.setType("ASSIGNMENT");
        feedPost.setAuthor(lecturer);
        feedPost.setCourse(course);
        feedPost.setCreatedAt(LocalDateTime.now());
        feedPost.setVisibility(visibility);
        Post savedPost = postRepository.save(feedPost);

        // File upload is separate and optional
            if (file != null && !file.isEmpty()) {
                try {
                    materialService.saveMaterial(course.getId(), title,
                            file.getContentType(), visibility, description, file, lecturer);
                    Attachment attachment = fileStorageService.storeFile(file);
                    attachment.setPost(savedPost);
                    String originalName = file.getOriginalFilename();
                    if (originalName != null && originalName.contains(".")) {
                        attachment.setFileType(
                            originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase()
                        );
                    }

                    System.out.println(">>> SAVING ASSIGNMENT ATTACHMENT: " + originalName
                        + " | type=" + attachment.getFileType());

                    attachmentRepository.save(attachment);
                    System.out.println(">>> ASSIGNMENT ATTACHMENT SAVED OK");
                        } catch (IOException e) {
                            redirectAttributes.addFlashAttribute("error",
                                    "Assignment created but file upload failed: " + e.getMessage());
                            return "redirect:/lecturer/assignments";
                        }
            }

        redirectAttributes.addFlashAttribute("success", "Assignment created successfully");
        return "redirect:/lecturer/assignments";
            }
    
      @GetMapping("/assignment/{id}/grade")
    public String gradeAssignmentPage(@PathVariable Long id,
                                      @RequestParam(required = false) Long submission,
                                      Model model,
                                      Authentication authentication) {
        User lecturer = userService.findByEmail(authentication.getName());
        Assignment assignment = assignmentService.getAssignmentById(id);
        
        // Verify lecturer owns this assignment
        if (!assignment.getLecturer().getId().equals(lecturer.getId())) {
            return "redirect:/lecturer/assignments";
        }
        
        List<Submission> submissions = submissionRepository.findByAssignmentOrderBySubmittedAtDesc(assignment);
        Submission currentSubmission = null;
        
        if (submission != null) {
            currentSubmission = submissionRepository.findById(submission).orElse(null);
        } else if (!submissions.isEmpty()) {
            // Find first ungraded submission
            currentSubmission = submissions.stream()
                    .filter(s -> !s.getGraded())
                    .findFirst()
                    .orElse(submissions.get(0));
        }
        
        long gradedCount = submissions.stream().filter(Submission::getGraded).count();
        long pendingCount = submissions.size() - gradedCount;
        
        double averageScore = submissions.stream()
                .filter(s -> s.getGraded() && s.getScore() != null)
                .mapToInt(Submission::getScore)
                .average()
                .orElse(0);
        
        model.addAttribute("user", lecturer);
        model.addAttribute("assignment", assignment);
        model.addAttribute("submissions", submissions);
        model.addAttribute("currentSubmission", currentSubmission);
        model.addAttribute("totalSubmissions", submissions.size());
        model.addAttribute("gradedCount", gradedCount);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("averageScore", Math.round(averageScore));
        model.addAttribute("hasPrevious", currentSubmission != null && submissions.indexOf(currentSubmission) > 0);
        model.addAttribute("hasNext", currentSubmission != null && submissions.indexOf(currentSubmission) < submissions.size() - 1);
        model.addAttribute("currentIndex", currentSubmission != null ? submissions.indexOf(currentSubmission) : -1);
        model.addAttribute("unreadMessages", messageService.countUnread(lecturer));
        model.addAttribute("unreadNotifications", notificationService.countUnread(lecturer));
        
        return "lecturer-grade-assignment";
    }

   
    @PostMapping("/assignment/{id}/submit-grade")
    public String submitGrade(@PathVariable Long id,
                              @RequestParam Long submissionId,
                              @RequestParam Integer score,
                              @RequestParam String feedback,
                              @RequestParam(required = false) String action,
                              @RequestParam(required = false, defaultValue = "false") boolean notifyStudent,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        User lecturer = userService.findByEmail(authentication.getName());
        
        try {
            Submission submission = submissionRepository.findById(submissionId).orElseThrow();
            submission.setScore(score);
            submission.setFeedback(feedback);
            submission.setGraded(true);
            submission.setGradedAt(LocalDateTime.now());
            submission.setGradedBy(lecturer);
            
            submissionRepository.save(submission);
            
            if (notifyStudent) {
                notificationService.sendGradeNotification(submission);
            }
            
            redirectAttributes.addFlashAttribute("success", "Grade saved successfully");
            
            // Determine redirect based on action
            if ("save_next".equals(action)) {
                // Find next ungraded submission
                List<Submission> submissions = submissionRepository.findByAssignment(submission.getAssignment());
                Optional<Submission> nextUngraded = submissions.stream()
                        .filter(s -> !s.getGraded() && !s.getId().equals(submissionId))
                        .findFirst();
                
                if (nextUngraded.isPresent()) {
                    return "redirect:/lecturer/assignment/" + id + "/grade?submission=" + nextUngraded.get().getId();
                }
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to save grade: " + e.getMessage());
        }
        
        return "redirect:/lecturer/assignment/" + id + "/grade";
    }

    @GetMapping("/assignment/{id}/submissions")
    public String viewSubmissions(@PathVariable Long id, Model model, Authentication authentication) {
        User lecturer = userService.findByEmail(authentication.getName());
        Course course = courseService.getCourseById(id); // use 'id'

        long count = assignmentService.countPendingForCourse(course);

        List<Assignment> assignments = assignmentService.getCourseAssignments(course.getId());
        if (assignments.isEmpty()) {
            return "redirect:/lecturer/assignments"; // no assignments for this course
        }

        Assignment assignment = assignments.get(0); // pick first

        if (!assignment.getCourse().getLecturer().getId().equals(lecturer.getId())) {
            return "redirect:/lecturer/assignments";
        }

        List<Submission> submissions = submissionRepository.findByAssignment(assignment);

        model.addAttribute("user", lecturer);
        model.addAttribute("assignment", assignment);
        model.addAttribute("submissions", submissions);
        model.addAttribute("gradedCount", submissions.stream().filter(Submission::getGraded).count());

        return "lecturer-submissions";
    }
    @GetMapping("/download/{storedFileName}")
    public void downloadFile(@PathVariable String storedFileName, HttpServletResponse response) throws IOException {
        Resource resource = fileStorageService.loadFileAsResource(storedFileName);

        String contentType = fileStorageService.getContentType(
                storedFileName.substring(storedFileName.lastIndexOf(".") + 1)
        );

        response.setContentType(contentType);
        response.setHeader("Content-Disposition", "attachment; filename=\"" + resource.getFilename() + "\"");
        Files.copy(resource.getFile().toPath(), response.getOutputStream());
        response.getOutputStream().flush();
    }

    // ==================== DISCUSSIONS MANAGEMENT ====================

   @GetMapping("/discussions")
    public String discussions(Model model, Authentication authentication) {
        User lecturer = userService.findByEmail(authentication.getName());
        List<Course> courses = courseService.getCoursesForLecturer(lecturer);

        // Correct: fetch discussion posts
        List<Post> discussions = postRepository.findByCourseInAndType(courses, "DISCUSSION");

        int unansweredCount = postRepository.countUnansweredDiscussionsForLecturer(lecturer);

        model.addAttribute("user", lecturer);
        model.addAttribute("discussions", discussions);
        model.addAttribute("unansweredCount", unansweredCount);
        model.addAttribute("courses", courses);
        model.addAttribute("unreadMessages", messageService.countUnread(lecturer));
        model.addAttribute("unreadNotifications", notificationService.countUnread(lecturer));

        return "lecturer-discussions";
    }

    @PostMapping("/discussion/{id}/reply")
    public String replyToDiscussion(@PathVariable Long id,
                                    @RequestParam String content,
                                    Authentication authentication) {
        User lecturer = userService.findByEmail(authentication.getName());
        Post discussion = postRepository.findById(id).orElseThrow();
        
        Post reply = new Post();
        reply.setTitle("Re: " + discussion.getTitle());
        reply.setContent(content);
        reply.setType("DISCUSSION");
        reply.setAuthor(lecturer);
        reply.setCourse(discussion.getCourse());
        reply.setParentPost(discussion);
        reply.setCreatedAt(LocalDateTime.now());
        
        postRepository.save(reply);
        
        return "redirect:/lecturer/discussions#discussion-" + id;
    }

    // ==================== ANNOUNCEMENTS ====================

    @GetMapping("/announcements")
    public String announcements(Model model, Authentication authentication) {
        User lecturer = userService.findByEmail(authentication.getName());
        List<Course> courses = courseService.getCoursesForLecturer(lecturer);
        
        List<Post> announcements = postRepository.findAnnouncementsByLecturer(lecturer);
        
        model.addAttribute("user", lecturer);
        model.addAttribute("announcements", announcements);
        model.addAttribute("courses", courses);
        model.addAttribute("unreadMessages", messageService.countUnread(lecturer));
        model.addAttribute("unreadNotifications", notificationService.countUnread(lecturer));
        
        return "lecturer-announcements";
    }

    @PostMapping("/announcement/create")
    public String createAnnouncement(Authentication authentication,
                                     @RequestParam Long courseId,
                                     @RequestParam String title,
                                     @RequestParam String content,
                                     @RequestParam(required = false) String visibility,
                                     @RequestParam(required = false, defaultValue = "false") boolean sendEmail,
                                     RedirectAttributes redirectAttributes) {
        User lecturer = userService.findByEmail(authentication.getName());
        Course course = courseService.getCourseById(courseId);
        
        Post announcement = new Post();
        announcement.setTitle(title);
        announcement.setContent(content);
        announcement.setType("ANNOUNCEMENT");
        announcement.setAuthor(lecturer);
        announcement.setCourse(course);
        announcement.setCreatedAt(LocalDateTime.now());
        announcement.setVisibility(visibility != null ? visibility : "COURSE");
        
        postRepository.save(announcement);
        
        if (sendEmail) {
            notificationService.sendAnnouncementEmail(announcement, course.getStudents());
        }
        
        redirectAttributes.addFlashAttribute("success", "Announcement posted successfully");
        
        return "redirect:/lecturer/announcements";
    }

    // ==================== ANALYTICS ====================

    @GetMapping("/analytics")
    public String analytics(Model model, Authentication authentication) {
        User lecturer = userService.findByEmail(authentication.getName());
        List<Course> courses = courseService.getCoursesForLecturer(lecturer);
        
        Map<String, Object> analyticsData = new HashMap<>();
        
        // Course performance metrics
        for (Course course : courses) {
            Map<String, Object> courseMetrics = new HashMap<>();
            courseMetrics.put("students", course.getStudents().size());
            courseMetrics.put("materials", materialService.countMaterialsByCourse(course.getId()));
            courseMetrics.put("assignments", assignmentService.countByCourse(course));
            courseMetrics.put("avgScore", assignmentService.getAverageScoreForCourse(course));
            courseMetrics.put("submissionRate", assignmentService.getSubmissionRateForCourse(course));
            
            analyticsData.put(course.getCode(), courseMetrics);
        }
        
        model.addAttribute("user", lecturer);
        model.addAttribute("courses", courses);
        model.addAttribute("analytics", analyticsData);
        model.addAttribute("totalStudents", courses.stream().mapToInt(c -> c.getStudents().size()).sum());
        model.addAttribute("totalMaterials", materialService.countMaterialsByLecturer(lecturer));
        model.addAttribute("totalAssignments", assignmentService.countByLecturer(lecturer));
        model.addAttribute("unreadMessages", messageService.countUnread(lecturer));
        model.addAttribute("unreadNotifications", notificationService.countUnread(lecturer));
        
        return "lecturer-analytics";
    }

    @GetMapping("/analytics/course/{id}")
    @ResponseBody
    public Map<String, Object> courseAnalytics(@PathVariable Long id, Authentication authentication) {
        User lecturer = userService.findByEmail(authentication.getName());
        Course course = courseService.getCourseById(id);
        
        if (!course.getLecturer().getId().equals(lecturer.getId())) {
            return Map.of("error", "Unauthorized");
        }
        
        Map<String, Object> analytics = new HashMap<>();
        analytics.put("submissionTrends", assignmentService.getSubmissionTrendsForCourse(course));
        analytics.put("gradeDistribution", assignmentService.getGradeDistributionForCourse(course));
        analytics.put("engagementMetrics", materialService.getEngagementMetricsForCourse(course));
        
        return analytics;
    }

    // ==================== PROFILE MANAGEMENT ====================

    @GetMapping("/profile")
    public String profile(Model model, Authentication authentication) {
        User lecturer = userService.findByEmail(authentication.getName());
        
        model.addAttribute("user", lecturer);
        model.addAttribute("courses", courseService.getCoursesForLecturer(lecturer));
        model.addAttribute("recentActivity", buildRecentActivity(lecturer, courseService.getCoursesForLecturer(lecturer)));
        model.addAttribute("unreadMessages", messageService.countUnread(lecturer));
        model.addAttribute("unreadNotifications", notificationService.countUnread(lecturer));
        
        return "lecturer-profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(Authentication authentication,
                                @RequestParam String fullName,
                                @RequestParam(required = false) String phoneNumber,
                                @RequestParam(required = false) String department,
                                @RequestParam(required = false) String bio,
                                @RequestParam(required = false) MultipartFile profilePicture,
                                RedirectAttributes redirectAttributes) {
        User lecturer = userService.findByEmail(authentication.getName());
        
        lecturer.setFullName(fullName);
        lecturer.setPhoneNumber(phoneNumber);
        lecturer.setDepartment(department);
        lecturer.setBio(bio);
        
        if (profilePicture != null && !profilePicture.isEmpty()) {
            try {
                lecturer.setProfilePicture(profilePicture.getBytes());
            } catch (IOException e) {
                redirectAttributes.addFlashAttribute("error", "Failed to upload profile picture");
            }
        }
        
        userService.saveUser(lecturer);
        redirectAttributes.addFlashAttribute("success", "Profile updated successfully");
        
        return "redirect:/lecturer/profile";
    }

    // ==================== MESSAGES & NOTIFICATIONS ====================

    @GetMapping("/messages")
    public String messages(Model model, Authentication authentication) {
        User lecturer = userService.findByEmail(authentication.getName());

        List<Course> myCourses = courseService.getCoursesForLecturer(lecturer);
        List<User> coursemates = myCourses.isEmpty()
            ? new ArrayList<>()
            : courseRepository.findCoursematesInCourses(myCourses, lecturer);

        Set<Long> onlineUserIds = messageService.getConversationsForUser(lecturer)
            .keySet().stream()
            .filter(u -> userStatusService.isOnline(u))
            .map(User::getId)
            .collect(Collectors.toSet());

        Map<Long, Integer> unreadPerUser = new HashMap<>();
        messageService.getConversationsForUser(lecturer).keySet().forEach(u ->
            unreadPerUser.put(u.getId(), messageService.countUnreadFromUser(u, lecturer))
        );

        model.addAttribute("onlineUserIds", onlineUserIds);
        model.addAttribute("unreadPerUser", unreadPerUser);
        model.addAttribute("user", lecturer);
        model.addAttribute("other", null);
        model.addAttribute("conversations", messageService.getConversationsForUser(lecturer));
        model.addAttribute("unreadMessages", messageService.countUnread(lecturer));
        model.addAttribute("unreadNotifications", notificationService.countUnread(lecturer));
        model.addAttribute("allUsersExceptCurrent", userService.getAllUsersExcept(lecturer));

        return "lecturer-messages";
    }

    @GetMapping("/messages/{userId}")
    @Transactional
    public String conversation(@PathVariable Long userId, Model model,
                               Authentication authentication) {
        User lecturer = userService.findByEmail(authentication.getName());
        User other = userService.getUserById(userId);

        messageService.markConversationAsRead(lecturer, other);

        Set<Long> onlineUserIds = messageService.getConversationsForUser(lecturer)
            .keySet().stream()
            .filter(u -> userStatusService.isOnline(u))
            .map(User::getId)
            .collect(Collectors.toSet());

        Map<Long, Integer> unreadPerUser = new HashMap<>();
        messageService.getConversationsForUser(lecturer).keySet().forEach(u ->
            unreadPerUser.put(u.getId(), messageService.countUnreadFromUser(u, lecturer))
        );

        model.addAttribute("onlineUserIds", onlineUserIds);
        model.addAttribute("conversations", messageService.getConversationsForUser(lecturer));
        model.addAttribute("unreadPerUser", unreadPerUser);
        model.addAttribute("user", lecturer);
        model.addAttribute("other", other);
        model.addAttribute("messages", messageService.getConversation(lecturer, other));
        model.addAttribute("unreadMessages", messageService.countUnread(lecturer));
        model.addAttribute("unreadNotifications", notificationService.countUnread(lecturer));
        model.addAttribute("isOtherOnline", userStatusService.isOnline(other));
        model.addAttribute("allUsersExceptCurrent", userService.getAllUsersExcept(lecturer));

        return "lecturer-conversation";
    }

    @GetMapping("/messages/{id}/refresh")
    @ResponseBody
    public String refresh(@PathVariable Long id, Authentication auth, Model model) {
        User lecturer = userService.findByEmail(auth.getName());
        User other = userService.getUserById(id);

        model.addAttribute("messages", messageService.getConversation(lecturer, other));

        return "fragments/message-list :: messages";
    }

    @PostMapping("/messages/{id}/read")
    @ResponseBody
    public void markRead(@PathVariable Long id, Authentication authentication) {
        User lecturer = userService.findByEmail(authentication.getName());
        User other = userService.getUserById(id);
        messageService.markConversationAsRead(lecturer, other);
    }

    @PostMapping("/messages/typing")
    @ResponseBody
    public void typing(@RequestBody Map<String, Object> body,
                       Authentication authentication) {
        User lecturer = userService.findByEmail(authentication.getName());
        Long recipientId = Long.valueOf(body.get("recipientId").toString());
        sseController.pushTyping(recipientId, lecturer.getId());
    }

    @GetMapping("/messages/new")
    public String newMessage(@RequestParam(required = false) Long student,
                             Model model,
                             Authentication authentication) {
        User lecturer = userService.findByEmail(authentication.getName());
        
        model.addAttribute("user", lecturer);
        model.addAttribute("recipientId", student);
        model.addAttribute("courses", courseService.getCoursesForLecturer(lecturer));
        model.addAttribute("students", getStudentsFromCourses(lecturer));
        
        return "lecturer-new-message";
    }

    @PostMapping("/messages/send")
    public String sendMessage(Authentication authentication,
                              @RequestParam Long recipientId,
                              @RequestParam(required = false, defaultValue = "Message") String subject,
                              @RequestParam String content,
                              RedirectAttributes redirectAttributes) {
        User lecturer = userService.findByEmail(authentication.getName());
        User recipient = userService.getUserById(recipientId);
        
        messageService.sendMessage(lecturer, recipient, content, subject);
        redirectAttributes.addFlashAttribute("success", "Message sent successfully");
        
        return "redirect:/lecturer/messages/" + recipientId;
    }

    @GetMapping("/notifications")
    public String notifications(Model model, Authentication authentication) {
        User lecturer = userService.findByEmail(authentication.getName());
        
        model.addAttribute("user", lecturer);
        model.addAttribute("notifications", notificationService.getNotificationsForUser(lecturer));
        model.addAttribute("unreadNotifications", notificationService.countUnread(lecturer));
        
        return "lecturer-notifications";
    }

    @PostMapping("/notifications/mark-read")
    @ResponseBody
    public ResponseEntity<?> markNotificationsRead(Authentication authentication) {
        User lecturer = userService.findByEmail(authentication.getName());
        notificationService.markAllAsRead(lecturer);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // ==================== EXPORT FUNCTIONS ====================

    @GetMapping("/course/{id}/export/grades")
    public ResponseEntity<byte[]> exportGrades(@PathVariable Long id, Authentication authentication) {
        User lecturer = userService.findByEmail(authentication.getName());
        Course course = courseService.getCourseById(id);
        
        if (!course.getLecturer().getId().equals(lecturer.getId())) {
            return ResponseEntity.badRequest().build();
        }
        
        byte[] csvData = assignmentService.generateGradeReport(course);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + course.getCode() + "_grades.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvData);
    }

    @GetMapping("/course/{id}/export/roster")
    public ResponseEntity<byte[]> exportRoster(@PathVariable Long id, Authentication authentication) {
        User lecturer = userService.findByEmail(authentication.getName());
        Course course = courseService.getCourseById(id);
        
        if (!course.getLecturer().getId().equals(lecturer.getId())) {
            return ResponseEntity.badRequest().build();
        }
        
        byte[] csvData = courseService.generateRosterReport(course);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + course.getCode() + "_roster.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvData);
    }

    // ==================== HELPER METHODS ====================

    
    private List<Map<String, Object>> buildCourseRecentActivity(Course course) {
        List<Map<String, Object>> activities = new ArrayList<>();
        
        // Recent submissions
        assignmentService.getRecentSubmissionsForCourse(course, 3)
            .forEach(submission -> activities.add(Map.of(
                    "icon", "fas fa-file-upload",
                    "text", submission.getStudent().getFullName() + " submitted " + submission.getAssignment().getTitle(),
                    "time", timeAgo(submission.getSubmittedAt()))
            ));
        // Recent posts
        postRepository.findByCourse(course).stream().limit(3)
                .forEach(p -> activities.add(Map.of(
                        "icon", iconForType(p.getType()),
                        "text", p.getTitle(),
                        "time", timeAgo(p.getCreatedAt()))));
        
        return activities;
    }

   
    private List<Integer> getEngagementDataForCourse(Course course, String period) {
        // Implement actual engagement data retrieval
        // This is placeholder data
        int points = period.equals("month") ? 4 : period.equals("semester") ? 6 : 7;
        Random rng = new Random(course.getId());
        List<Integer> data = new ArrayList<>();
        for (int i = 0; i < points; i++) {
            data.add(25 + rng.nextInt(70));
        }
        return data;
    }

    
    private String calculateStorageUsed(List<Material> materials) {
        double totalMB = materials.stream()
                .mapToDouble(Material::getFileSize)
                .sum();
        
        if (totalMB < 1024) {
            return String.format("%.1f MB", totalMB);
        } else {
            return String.format("%.2f GB", totalMB / 1024);
        }
    }

    private Set<User> getStudentsFromCourses(User lecturer) {
        return courseService.getCoursesForLecturer(lecturer).stream()
                .flatMap(c -> c.getStudents().stream())
                .collect(Collectors.toSet());
    }
    @GetMapping("/course/{id}/assignments")
    public String viewCourseAssignments(@PathVariable Long id, Model model, Authentication authentication) {
        User lecturer = userService.findByEmail(authentication.getName());
        Course course = courseService.getCourseById(id); // get the course by path variable

        // Check that the lecturer owns this course
        if (!course.getLecturer().getId().equals(lecturer.getId())) {
            return "redirect:/lecturer/assignments";
        }

        // Get all assignments for the course
        List<Assignment> assignments = assignmentService.getCourseAssignments(course.getId());

        // Prepare counts per assignment
        Map<Long, Long> pendingCounts = new LinkedHashMap<>();
        Map<Long, Long> gradedCounts = new LinkedHashMap<>();

        for (Assignment assignment : assignments) {
            // Count pending and graded submissions per assignment
            long pending = assignmentService.hasPendingGrading(assignment) ? 1 : 0; // or implement proper count
            long graded = submissionRepository.findByAssignmentOrderBySubmittedAtDesc(assignment)
                                             .stream().filter(Submission::getGraded).count();

            pendingCounts.put(assignment.getId(), pending);
            gradedCounts.put(assignment.getId(), graded);
        }

        model.addAttribute("user", lecturer);
        model.addAttribute("course", course);
        model.addAttribute("assignments", assignments);
        model.addAttribute("pendingCounts", pendingCounts);
        model.addAttribute("gradedCounts", gradedCounts);

        return "lecturer-course-assignments";
    }
    @GetMapping("/courses/{courseId}/schedule")
    public String editCourseSchedule(@PathVariable Long courseId, Model model,
                                     @AuthenticationPrincipal User currentUser,Authentication authentication) {
        User lecturer = userService.findByEmail(authentication.getName());
        Course course = courseService.getCourseById(courseId);

        // Optional: check if currentUser is actually the lecturer for this course
        if (!course.getLecturer().equals(currentUser)) {
            return "redirect:/lecturer/courses"; // no access
        }

        model.addAttribute("course", course);
        return "lecturer-edit-schedule"; // Thymeleaf template for editing schedule
    }
    // Add these methods to your controller

    @PostMapping("/courses/{courseId}/schedule/add")
    public String addCourseSchedule(@PathVariable Long courseId,
                                   @RequestParam LocalDateTime startTime,
                                   @RequestParam LocalDateTime endTime,
                                   @AuthenticationPrincipal User currentUser) {
        Course course = courseService.getCourseById(courseId);

        // Verify lecturer owns this course
        if (!course.getLecturer().equals(currentUser)) {
            return "redirect:/lecturer/courses";
        }

        CourseSchedule schedule = new CourseSchedule();
        schedule.setCourse(course);
        schedule.setLecturer(currentUser);
        schedule.setStartTime(startTime);
        schedule.setEndTime(endTime);

        courseService.addCourseSchedule(course, currentUser, startTime, endTime);

        return "redirect:/lecturer/courses/" + courseId + "/schedule";
    }

    @PostMapping("/courses/{courseId}/schedule/update")
    public String updateCourseSchedule(@PathVariable Long courseId,
                                      @RequestParam Long scheduleId,
                                      @RequestParam LocalDateTime startTime,
                                      @RequestParam LocalDateTime endTime,
                                      @AuthenticationPrincipal User currentUser) {
        CourseSchedule schedule = courseService.getScheduleById(scheduleId);

        // Verify permissions
        if (!schedule.getCourse().getId().equals(courseId) || 
            !schedule.getLecturer().equals(currentUser)) {
            return "redirect:/lecturer/courses";
        }

        schedule.setStartTime(startTime);
        schedule.setEndTime(endTime);
        courseService.updateCourseSchedule(courseId, scheduleId, currentUser, startTime, endTime);


        return "redirect:/lecturer/courses/" + courseId + "/schedule";
    }

    @PostMapping("/courses/{courseId}/schedule/delete/{scheduleId}")
    public String deleteCourseSchedule(@PathVariable Long courseId,
                                      @PathVariable Long scheduleId,
                                      @AuthenticationPrincipal User currentUser) {
        CourseSchedule schedule = courseService.getScheduleById(scheduleId);

        // Verify permissions
        if (!schedule.getCourse().getId().equals(courseId) || 
            !schedule.getLecturer().equals(currentUser)) {
            return "redirect:/lecturer/courses";
        }

        courseService.deleteCourseSchedule(courseId, scheduleId, currentUser);


        return "redirect:/lecturer/courses/" + courseId + "/schedule";
    }
    @GetMapping("/courses/{courseId}/materials/upload")
    public String uploadMaterialPage(@PathVariable Long courseId,
                                     Model model,
                                     Authentication authentication) {

        User lecturer = userService.findByEmail(authentication.getName());
        Course course = courseService.getCourseById(courseId);

        // امنیت check
        if (!course.getLecturer().getId().equals(lecturer.getId())) {
            return "redirect:/lecturer/courses";
        }

        model.addAttribute("course", course);
        model.addAttribute("user", lecturer);

        return "lecturer-upload-material"; // create this HTML
    }
    @GetMapping("/assignment/create")
    public String createAssignmentPage() {
        return "lecturer-create-assignment";
    }
    @GetMapping("/attendance/take")
    public String takeAttendancePage() {
        return "lecturer-attendance";
    }
   /* @PostMapping("/materials/upload")
    public String uploadCourseMaterial(
            @PathVariable Long courseId,
            @RequestParam("title") String title,
            @RequestParam("type") String type,
            @RequestParam(value = "visibility", defaultValue = "COURSE") String visibility,
            @RequestParam(value = "description", required = false, defaultValue = "") String description,
            @RequestParam("file") MultipartFile file,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        User lecturer = userService.findByEmail(authentication.getName());
        Course course = courseService.getCourseById(courseId);

        // Security check
        if (!course.getLecturer().getId().equals(lecturer.getId())) {
            return "redirect:/lecturer/courses";
        }

        try {
            materialService.saveMaterial(courseId, title, type, visibility, description, file, lecturer);

            // Create feed post so students see it
            Post feedPost = new Post();
            feedPost.setTitle(title);
            feedPost.setContent(description.isEmpty() ? "New material uploaded." : description);
            feedPost.setType("MATERIAL");
            feedPost.setAuthor(lecturer);
            feedPost.setCourse(course);
            feedPost.setCreatedAt(LocalDateTime.now());
            feedPost.setVisibility(visibility);
            postRepository.save(feedPost);

            redirectAttributes.addFlashAttribute("success", "Material uploaded successfully!");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Failed to upload: " + e.getMessage());
        }

        return "redirect:/lecturer/course/" + courseId;
    } */
    
    @PostMapping("/profile/upload")
    public String uploadProfilePicture(@RequestParam("file") MultipartFile file,
                                       Authentication authentication) throws Exception {

        User user = userService.findByEmail(authentication.getName());

        user.setProfilePicture(file.getBytes());
        user.setProfilePictureType(file.getContentType());

        userService.saveUser(user);

        return "redirect:/student/profile";
    }
    @GetMapping("/user/{id}/profile-picture")
    @ResponseBody
    public ResponseEntity<byte[]> getProfilePicture(@PathVariable Long id) {

        User user = userService.getUserById(id);

        return ResponseEntity.ok()
                .header("Content-Type", user.getProfilePictureType())
                .body(user.getProfilePicture());
    }

}