package com.finalyearproject.service;

import com.finalyearproject.model.*;
import com.finalyearproject.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PostService {

    private final PostRepository       postRepository;
    private final CommentRepository    commentRepository;
    private final AttachmentRepository attachmentRepository;
    private final FileStorageService   fileStorageService;
    private final CourseRepository     courseRepository;

    public PostService(PostRepository postRepository,
                       CommentRepository commentRepository,
                       AttachmentRepository attachmentRepository,
                       FileStorageService fileStorageService,
                       CourseRepository courseRepository) {
        this.postRepository       = postRepository;
        this.commentRepository    = commentRepository;
        this.attachmentRepository = attachmentRepository;
        this.fileStorageService   = fileStorageService;
        this.courseRepository     = courseRepository;
    }

    // ── GET POSTS FOR STUDENT ──────────────────────────────────────────────────
    public List<Post> getPostsForUser(User user, String filter) {
        // Always query DB — never rely on user.getCourses() lazy collection
        List<Course> courses = courseRepository.findCoursesWithDetailsByStudent(user);
        if (courses == null || courses.isEmpty()) return List.of();

        List<Post> posts;
        if (filter == null || filter.equals("ALL")) {
            posts = postRepository.findByCourseInOrVisibility(courses, "ALL");
        } else if (filter.equals("LECTURER")) {
            posts = postRepository.findLecturerPostsForCourses(courses);
        } else {
            posts = postRepository.findByCourseInAndType(courses, filter);
        }

        for (Post post : posts) {
            post.setUserLiked(post.getLikedBy() != null && post.getLikedBy().contains(user));
            post.setUserSaved(post.getSavedBy() != null && post.getSavedBy().contains(user));
        }
        return posts;
    }

    public List<Post> getPostsForUser(User user) {
        return getPostsForUser(user, "ALL");
    }

    // ── CREATE POST (8-arg — default visibility COURSE) ───────────────────────
    @Transactional
    public Post createPost(User author, String title, String content,
                           String type, Long courseId,
                           MultipartFile file, String linkUrl,
                           List<Course> userCourses) throws IOException {
        return createPost(author, title, content, type, courseId,
                          file, linkUrl, userCourses, "COURSE");
    }

    // ── CREATE POST (9-arg — explicit visibility) ──────────────────────────────
    @Transactional
    public Post createPost(User author, String title, String content,
                           String type, Long courseId,
                           MultipartFile file, String linkUrl,
                           List<Course> userCourses,
                           String visibility) throws IOException {

        Post post = new Post();
        post.setAuthor(author);
        post.setTitle(title != null ? title : "");
        post.setContent(content);
        post.setType(type != null ? type.toUpperCase() : "TEXT");
        post.setCreatedAt(LocalDateTime.now());
        post.setLikes(0);
        post.setViews(0);
        post.setVisibility(visibility != null ? visibility.toUpperCase() : "COURSE");

        if (courseId != null && userCourses != null) {
            userCourses.stream()
                    .filter(c -> c.getId().equals(courseId))
                    .findFirst()
                    .ifPresent(post::setCourse);
        }

        if ("LINK".equalsIgnoreCase(type) && linkUrl != null && !linkUrl.isBlank()) {
            post.setLinkUrl(linkUrl);
        }

        Post saved = postRepository.save(post);

        if (file != null && !file.isEmpty()) {
            try {
                Attachment attachment = fileStorageService.storeFile(file);
                attachment.setPost(saved);

                String originalName = file.getOriginalFilename();
                if (originalName != null && originalName.contains(".")) {
                    attachment.setFileType(
                        originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase()
                    );
                }

                System.out.println(">>> STORING: " + originalName
                    + " | type=" + attachment.getFileType()
                    + " | size=" + attachment.getFileSize()
                    + " | stored=" + attachment.getStoredFileName());

                Attachment savedAttachment = attachmentRepository.save(attachment);
                System.out.println(">>> SAVED OK, ID=" + savedAttachment.getId());

                if (saved.getAttachments() != null) {
                    saved.getAttachments().add(savedAttachment);
                } else {
                    System.err.println(">>> attachments list is NULL on Post!");
                }

            } catch (Exception e) {
                System.err.println(">>> ATTACHMENT FAILED: " 
                    + e.getClass().getName() + " — " + e.getMessage());
                e.printStackTrace();
            }
        }

        return saved;
    }
    
    // ── LIKE ───────────────────────────────────────────────────────────────────
    @Transactional
    public boolean toggleLike(Long postId, User user) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        boolean alreadyLiked = post.getLikedBy().contains(user);
        if (alreadyLiked) { post.getLikedBy().remove(user); post.setLikes(Math.max(0, post.getLikes() - 1)); }
        else              { post.getLikedBy().add(user);    post.setLikes(post.getLikes() + 1); }
        postRepository.save(post);
        return !alreadyLiked;
    }

    // ── SAVE ───────────────────────────────────────────────────────────────────
    @Transactional
    public boolean toggleSave(Long postId, User user) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        boolean alreadySaved = post.getSavedBy().contains(user);
        if (alreadySaved) post.getSavedBy().remove(user); else post.getSavedBy().add(user);
        postRepository.save(post);
        return !alreadySaved;
    }

    // ── COMMENT ────────────────────────────────────────────────────────────────
    @Transactional
    public Comment addComment(Long postId, User author, String text) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        Comment comment = new Comment();
        comment.setPost(post); comment.setAuthor(author);
        comment.setText(text); comment.setCreatedAt(LocalDateTime.now());
        return commentRepository.save(comment);
    }

    @Transactional
    public void incrementViews(Long postId) {
        postRepository.findById(postId).ifPresent(p -> { p.setViews(p.getViews() + 1); postRepository.save(p); });
    }

    @Transactional
    public void incrementDownload(Long attachmentId) {
        attachmentRepository.findById(attachmentId).ifPresent(a -> { a.setDownloadCount(a.getDownloadCount() + 1); attachmentRepository.save(a); });
    }

    public Post     getPostById(Long id) { return postRepository.findById(id).orElseThrow(() -> new RuntimeException("Post not found")); }
    public long     countAllPosts()      { return postRepository.count(); }
    public List<Post> getAllPosts()       { return postRepository.findAll(); }
    public void     sendAnnouncement(String title, String message) { System.out.println("Announcement: " + title); }
    public void     generateReport()     { System.out.println("Report generated."); }
    public int      getPostStatistics()  { return 42; }
    public List<Post> getSavedPostsForUser(User user) {
        return postRepository.findBySavedByContaining(user);
    }
}