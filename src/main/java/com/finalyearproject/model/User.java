
package com.finalyearproject.model;


import com.finalyearproject.model.Role.RoleType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true)
    private String universityId; // Student or Lecturer ID

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoleType role;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private boolean approved = false; // Lecturers must be approved by Admin

    @Column(nullable = true)
    private String faculty;
    
    @ManyToOne
    @JoinColumn(name = "lecturer_id")
    private User lecturer;    
    
    private String profilePictureType; // image/png, image/jpeg

    public String getProfilePictureType() {
        return profilePictureType;
    }

    public void setProfilePictureType(String profilePictureType) {
        this.profilePictureType = profilePictureType;
    }

    
    public User getLecturer() {
        return lecturer;
    }

    public void setLecturer(User lecturer) {
        this.lecturer = lecturer;
    }

    
    private LocalDateTime lastLogin;
    @Transient
    public String getLastActiveFormatted() {
        if (lastLogin == null) {
            return "Never";
        }
        return lastLogin.format(DateTimeFormatter.ofPattern("HH:mm, MMM dd"));
    }
    @Transient
    public String getStatus() {
        if (!approved) {
            return "PENDING";
        }
        return "ACTIVE";
    }
    
    
    @ManyToMany
    @JoinTable(
        name = "user_materials",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "material_id")
    )
    private List<Material> materials;
    
     @ManyToMany
    @JoinTable(
        name = "user_courses",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    private List<Course> courses;
     
    private LocalDateTime createdAt;
    // User.java
    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "department")
    private String department;

    @Column(name = "bio", length = 1000)
    private String bio;

    @Column(name = "profile_picture", columnDefinition = "bytea")
    private byte[] profilePicture;

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getDepartment() {
        return department;
    }

    public String getBio() {
        return bio;
    }

    public byte[] getProfilePicture() {
        return profilePicture;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
    public List<Course> getCourses() {
        return courses;
    }

    public void setCourses(List<Course> courses) {
        this.courses = courses;
    }


    public List<Material> getMaterials() {
        return materials;
    }

    public void setMaterials(List<Material> materials) {
        this.materials = materials;
    }

    public User() {}
    

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUniversityId() { return universityId; }
    public void setUniversityId(String universityId) { this.universityId = universityId; }

    public RoleType getRole() { return role; }
    public void setRole(RoleType role) { this.role = role; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }

    public String getFaculty() { return faculty; }
    public void setFaculty(String faculty) { this.faculty = faculty; }
    public void setPhoneNumber(String phoneNumber) {
    this.phoneNumber = phoneNumber;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }
   public void setProfilePicture(byte[] profilePicture) {
        this.profilePicture = profilePicture;
    }
   @Transient
    public double getEngagement() {
        if (!approved) {
            return 0; // Not approved users are considered non-engaged
        }

        int coursesCount = courses != null ? courses.size() : 0;
        int materialsCount = materials != null ? materials.size() : 0;

        // Weight factors
        double courseWeight = 2.0;
        double materialWeight = 1.0;
        double recencyWeight = 3.0;

        // Recency score: decays after 7 days
        double recencyScore = 0;
        if (lastLogin != null) {
            long daysSinceLastLogin = java.time.Duration.between(lastLogin, java.time.LocalDateTime.now()).toDays();
            recencyScore = Math.max(0, 7 - daysSinceLastLogin); // Max 7, decays daily
        }

        return coursesCount * courseWeight + materialsCount * materialWeight + recencyScore * recencyWeight;
    }

    @Transient
    public String getEngagementStatus() {
        double score = getEngagement();
        if (score == 0) return "INACTIVE";
        else if (score < 10) return "LOW";
        else if (score < 20) return "MEDIUM";
        else return "HIGH";
    }
}
