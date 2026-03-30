package com.finalyearproject.service;

import com.finalyearproject.model.User;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

@Service
public class DashboardService {

    private final MessageService messageService;
    private final NotificationService notificationService;
    private final AssignmentService assignmentService;
    private final SavedService savedService;
    private final CourseService courseService;
    private final PostService postService;
    private final DeadlineService deadlineService;
    private final UserStatusService userStatusService;
    private final MaterialService materialService;
    private final EngagementService engagementService;

    public DashboardService(MessageService messageService,
                            NotificationService notificationService,
                            AssignmentService assignmentService,
                            SavedService savedService,
                            CourseService courseService,
                            PostService postService,
                            DeadlineService deadlineService,
                            UserStatusService userStatusService,
                            MaterialService materialService,
                            EngagementService engagementService) {

        this.messageService = messageService;
        this.notificationService = notificationService;
        this.assignmentService = assignmentService;
        this.savedService = savedService;
        this.courseService = courseService;
        this.postService = postService;
        this.deadlineService = deadlineService;
        this.userStatusService = userStatusService;
        this.materialService = materialService;
        this.engagementService = engagementService;
    }

    public void populateDashboardModel(User user, Model model) {

        int unreadMessages = messageService.countUnread(user);
        int unreadNotifications = notificationService.countUnread(user);
        int pendingAssignments = assignmentService.countPending(user);
        int savedItems = savedService.countSaved(user);

        int coursesCount = courseService.countCoursesForUser(user);
        int materialsCount = materialService.countMaterialsForUser(user);
        int engagementPercent = engagementService.calculateEngagement(user);

        model.addAttribute("user", user);
        model.addAttribute("unreadMessages", unreadMessages);
        model.addAttribute("unreadNotifications", unreadNotifications);
        model.addAttribute("pendingAssignments", pendingAssignments);
        model.addAttribute("savedItems", savedItems);

        model.addAttribute("coursesCount", coursesCount);
        model.addAttribute("materialsCount", materialsCount);
        model.addAttribute("engagementPercent", engagementPercent);

        model.addAttribute("courses", courseService.getCoursesForUser(user));
        model.addAttribute("posts", postService.getPostsForUser(user));
        model.addAttribute("deadlines", deadlineService.getUpcomingDeadlinesForUser(user));
        model.addAttribute("peersOnline", userStatusService.getOnlinePeersForUser(user));
    }
}