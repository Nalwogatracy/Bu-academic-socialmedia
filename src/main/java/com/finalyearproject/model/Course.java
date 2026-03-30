
package com.finalyearproject.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "courses")
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;        // Software Engineering
    private String code;        // ICT 3101
    private String department;  // e.g., IT Department
    @ManyToOne
    private User lecturer; 
    private LocalDateTime createdAt;

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public User getLecturer() {
        return lecturer;
    }

    public void setLecturer(User lecturer) {
        this.lecturer = lecturer;
    }

    @ManyToMany(mappedBy = "courses")
    private Set<User> students = new HashSet<>();

    public Set<User> getStudents() {
        return students;
    }

    public void setStudents(Set<User> students) {
        this.students = students;
    }

   
    @Transient
    public int getStudentCount() {
        return students != null ? students.size() : 0;
    }
     @Transient
    public int getPostCount() {
        return posts != null ? posts.size() : 0;
    }
    @Transient
    public int getEngagement() {
        // example: engagement = number of posts for this course
        return posts != null ? posts.size() : 0;
    }
     @Transient
    private int materialsCount;

    public int getMaterialsCount() {
        return materialsCount;
    }

    public void setMaterialsCount(int materialsCount) {
        this.materialsCount = materialsCount;
    }

    
    @OneToMany(mappedBy = "course")
    private List<CourseEnrollment> enrollments;

    @OneToMany(mappedBy = "course")
    private Set<Material> materials = new HashSet<>();

    public Set<Material> getMaterials() {
        return materials;
    }

    public void setMaterials(Set<Material> materials) {
        this.materials = materials;
    }


    @OneToMany(mappedBy = "course")
    private List<Post> posts;
    
    @Transient
    private int postsCount;
    @Transient
    private String schedule;
    
    @OneToMany(mappedBy = "course")
    private List<CourseSchedule> schedules;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL)
    private List<Assignment> assignments = new ArrayList<>();

    public List<Assignment> getAssignments() {
        return assignments;
    }

    public void setAssignments(List<Assignment> assignments) {
        this.assignments = assignments;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule; // optional, only if you want to allow manual override
    }

    public String getSchedule() {
        if (schedules == null || schedules.isEmpty()) {
            return "TBA";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        StringBuilder sb = new StringBuilder();
        for (CourseSchedule s : schedules) {
        if (s.getStartTime() != null && s.getEndTime() != null) {
            sb.append(s.getStartTime())
              .append(" - ")
              .append(s.getEndTime())
              .append("; ");
        }
        }
        // Remove trailing "; " for neatness
        if (sb.length() > 2) {
            sb.setLength(sb.length() - 2);
        }

        return sb.length() > 0 ? sb.toString() : "TBA";
    }
    public int getPostsCount() {
        return postsCount;
    }

    public void setPostsCount(int postsCount) {
        this.postsCount = postsCount;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public List getEnrollments() {
        return enrollments;
    }

    public void setEnrollments(List enrollments) {
        this.enrollments = enrollments;
    }


    public List getPosts() {
        return posts;
    }

    public void setPosts(List posts) {
        this.posts = posts;
    }

   @Transient
    private String description;
    public String getDescription() {
        if (description != null) {
            return description; // if manually set
        }

        // Example dynamic description
        StringBuilder sb = new StringBuilder();
        sb.append("Course ").append(name != null ? name : "N/A")
          .append(" (").append(code != null ? code : "N/A").append(")")
          .append(" in ").append(department != null ? department : "N/A")
          .append(". Students enrolled: ").append(getStudentCount())
          .append(". Posts: ").append(getPostCount());

        return sb.toString();
    }
    @Transient
    public int getDiscussionCount() {
        if (posts == null) return 0;

        return (int) posts.stream()
                .filter(p -> p.getType() != null && p.getType().equalsIgnoreCase("DISCUSSION"))
                .count();
    }
    @Transient
    private int unreadCount;

    // getter & setter
    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }
}
