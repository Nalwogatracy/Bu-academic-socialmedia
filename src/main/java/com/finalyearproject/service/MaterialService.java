package com.finalyearproject.service;

import com.finalyearproject.model.Course;
import com.finalyearproject.model.Material;
import com.finalyearproject.model.User;
import com.finalyearproject.repository.CourseRepository;
import com.finalyearproject.repository.MaterialRepository;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MaterialService {

    private final MaterialRepository materialRepository;
    
    @Autowired
    private CourseRepository courseRepository;

    public MaterialService(MaterialRepository materialRepository) {
        this.materialRepository = materialRepository;
    }

    // Count total materials uploaded for a user (across their courses)
    public int countMaterialsForUser(User user) {
        return materialRepository.countByUploadedBy(user);
    }

    // Get all materials accessible by a user (their courses)
    public List<Material> getMaterialsForUser(User user) {
        return materialRepository.findByUploadedBy(user);
    }

    // Save new material
    public Material saveMaterial(Long courseId, String title, String type, String visibility,
                                 String description, MultipartFile file, User uploadedBy) throws IOException {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        Material material = new Material();
        material.setCourse(course);
        material.setTitle(title);
        material.setFileType(type);
        material.setVisibility(visibility);
        material.setDescription(description);
        material.setFileName(file.getOriginalFilename());
        material.setFileData(file.getBytes()); // store file bytes
        material.setFileSize(file.getSize() / (1024.0 * 1024.0));
        material.setUploadedAt(LocalDateTime.now());
        material.setUploadedBy(uploadedBy);

        return materialRepository.save(material);
    }

    // Delete material
    public void deleteMaterial(Long id) {
        materialRepository.deleteById(id);
    }
    public List<Material> getCourseMaterials(Long courseId) {
        return materialRepository.findByCourseId(courseId);
    }
    public long countMaterialsByLecturer(User lecturer) {
        return materialRepository.countByLecturer(lecturer);
    }
    public Material updateMaterial(Long id, String title, String type, String visibility, String description) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Material not found"));
        material.setTitle(title);
        material.setFileType(type);
        material.setVisibility(visibility);
        material.setDescription(description);
        return materialRepository.save(material);
    }
    public void downloadMaterial(Long id, HttpServletResponse response) throws IOException {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Material not found"));

        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + material.getFileName() + "\"");
        response.getOutputStream().write(material.getFileData());
        response.getOutputStream().flush();
    }
    public List<Material> getMaterialsForCourses(List<Course> courses) {
        return materialRepository.findByCourseIn(courses);
    }
    public long countMaterialsByCourse(Long courseId) {
        return materialRepository.countByCourseId(courseId);
    }
    public Material getMaterialById(Long id) {
        return materialRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Material not found"));
    }
    public Map<String, Object> getEngagementMetricsForCourse(Course course) {
        long materialCount = materialRepository.countByCourseId(course.getId());
        long totalSizeMB = (long) materialRepository.findByCourseId(course.getId())
                                .stream()
                                .mapToDouble(Material::getFileSize)
                                .sum();

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("materialCount", materialCount);
        metrics.put("totalSizeMB", totalSizeMB);
        return metrics;
    }

}