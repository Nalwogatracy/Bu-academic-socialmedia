
package com.finalyearproject.repository;

import com.finalyearproject.model.Course;
import com.finalyearproject.model.Material;
import com.finalyearproject.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MaterialRepository extends JpaRepository<Material, Long> {

    int countByUploadedBy(User user);

    List<Material> findByUploadedBy(User user);
    List<Material> findByCourseId(Long courseId);
    long countByLecturer(User lecturer);
    @Query("SELECT m FROM Material m WHERE m.course IN :courses ORDER BY m.uploadedAt DESC")
    List<Material> findByCourseIn(@Param("courses") List<Course> courses);
    long countByCourseId(Long courseId);
    @Query("SELECT COUNT(m) FROM Material m WHERE m.course IN :courses")
    long countByCourses(@Param("courses") List<Course> courses);
    
}
