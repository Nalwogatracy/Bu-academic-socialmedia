package com.finalyearproject.repository;

import com.finalyearproject.model.Course;
import com.finalyearproject.model.Post;
import com.finalyearproject.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    // ── All posts for a set of courses ─────────────────────────────────────────
    @Query("SELECT p FROM Post p WHERE p.course IN :courses ORDER BY p.createdAt DESC")
    List<Post> findByCourseIn(@Param("courses") List<Course> courses);

    // ── Posts filtered by type ─────────────────────────────────────────────────
    @Query("SELECT p FROM Post p WHERE p.course IN :courses AND p.type = :type ORDER BY p.createdAt DESC")
    List<Post> findByCourseInAndType(@Param("courses") List<Course> courses, @Param("type") String type);

    // ── Posts from LECTURER authors only ──────────────────────────────────────
    @Query("""
        SELECT p FROM Post p
        JOIN p.author a
        WHERE p.course IN :courses
        AND a.role = 'LECTURER'
        ORDER BY p.createdAt DESC
    """)
    List<Post> findLecturerPostsForCourses(@Param("courses") List<Course> courses);

    // ── "ALL" filter: enrolled-course posts + global announcements ─────────────
    /**
     * ╔══════════════════════════════════════════════════════════════════╗
     * ║  This is the main query for the student feed (filter = ALL).    ║
     * ║                                                                  ║
     * ║  Returns:                                                        ║
     * ║    1. Posts linked to any of the student's enrolled courses      ║
     * ║    2. Posts with visibility = 'ALL' (admin/global announcements) ║
     * ║                                                                  ║
     * ║  IMPORTANT: if courses list is empty the first condition         ║
     * ║  matches nothing, but admin "ALL" visibility posts still show.   ║
     * ╚══════════════════════════════════════════════════════════════════╝
     */
    @Query("""
        SELECT DISTINCT p FROM Post p
        WHERE p.course IN :courses
           OR p.visibility = :visibility
        ORDER BY p.createdAt DESC
    """)
    List<Post> findByCourseInOrVisibility(
            @Param("courses")    List<Course> courses,
            @Param("visibility") String visibility);

    // ── All posts by a specific author ─────────────────────────────────────────
    List<Post> findByAuthorOrderByCreatedAtDesc(User author);

    // ── Saved posts for a user ─────────────────────────────────────────────────
    @Query("SELECT p FROM Post p JOIN p.savedBy s WHERE s = :user ORDER BY p.createdAt DESC")
    List<Post> findSavedByUser(@Param("user") User user);

    // ── Unanswered discussions in a set of courses ─────────────────────────────
    @Query("""
        SELECT COUNT(p) FROM Post p
        WHERE p.course IN :courses
        AND p.type = 'DISCUSSION'
        AND SIZE(p.comments) = 0
    """)
    int countUnansweredDiscussions(@Param("courses") List<Course> courses);

    default int countUnansweredDiscussionsForLecturer(User lecturer) {
        return 0; // implemented in service layer via getCoursesForLecturer
    }
   
    // Optional: Find posts by a single course
    List<Post> findByCourse(Course course);
    
    @Query("SELECT p FROM Post p WHERE p.author = :lecturer AND p.type = 'ANNOUNCEMENT' ORDER BY p.createdAt DESC")
    List<Post> findAnnouncementsByLecturer(@Param("lecturer") User lecturer);
    
    List<Post> findBySavedByContaining(User user);
    // In PostRepository
    List<Post> findByCourseIsNullAndAuthorIn(List<User> authors);
}