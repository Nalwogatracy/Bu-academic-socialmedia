
package com.finalyearproject.controller;


import com.finalyearproject.model.Course;
import com.finalyearproject.model.Material;
import com.finalyearproject.model.User;
import com.finalyearproject.service.CourseService;
import com.finalyearproject.service.MaterialService;
import com.finalyearproject.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
@RequestMapping("/courses/{courseId}/materials")
public class CourseMaterialController {

    private final CourseService courseService;
    private final MaterialService materialService;
    private final UserService userService;

    @Autowired
    public CourseMaterialController(CourseService courseService, MaterialService materialService, UserService userService) {
        this.courseService = courseService;
        this.materialService = materialService;
        this.userService = userService;
    }

    // ── List materials ──
    @GetMapping
    public String listMaterials(@PathVariable Long courseId, Model model) {
        Course course = courseService.getCourseById(courseId);
        List<Material> materials = materialService.getCourseMaterials(courseId);

        model.addAttribute("course", course);
        model.addAttribute("materials", materials);

        return "admin/course-materials"; // your Thymeleaf template
    }

    // ── Upload material ──
   @PostMapping("/upload")
    public String uploadMaterial(@PathVariable Long courseId,
                                 @RequestParam String title,
                                 @RequestParam String type,
                                 @RequestParam String description,
                                 @RequestParam("file") MultipartFile file,
                                 Principal principal) {
        try {
            User user = userService.findByEmail(principal.getName());

            // call saveMaterial with all required parameters
            materialService.saveMaterial(
                courseId,           // courseId
                title,        // title
                type,         // type
                "public",     // visibility (or get from request)
                description,  // description
                file,         // MultipartFile
                user          // uploadedBy
            );

            return "redirect:/admin/courses/" + courseId;
        } catch (IOException e) {
            e.printStackTrace();
            return "redirect:/admin/courses/" + courseId + "?error=true";
        }
    }

    // ── Delete material ──
    @PostMapping("/{id}/delete")
    public String deleteMaterial(@PathVariable Long courseId, @PathVariable Long id) {
        materialService.deleteMaterial(id);
        return "redirect:/admin/courses/" + courseId + "/materials";
    }

    // ── Preview material ──
    @GetMapping("/{id}/preview")
    public String previewMaterial(@PathVariable Long id) {
        // Implementation depends on your file storage (e.g., generate URL or serve PDF/Video)
        return "redirect:/materials/preview/" + id;
    }

    // ── Download material ──
    @GetMapping("/{id}/download")
    public void downloadMaterial(@PathVariable Long id, HttpServletResponse response) throws IOException {
        materialService.downloadMaterial(id, response);
    }

    // ── Edit material (optional) ──
    @PostMapping("/{id}/edit")
    public String editMaterial(@PathVariable Long courseId, @PathVariable Long id,
                               @RequestParam("title") String title,
                               @RequestParam("type") String type,
                               @RequestParam(value = "visibility", required = false) String visibility,
                               @RequestParam(value = "description", required = false) String description) {
        materialService.updateMaterial(id, title, type, visibility, description);
        return "redirect:/admin/courses/" + courseId + "/materials";
    }
}
