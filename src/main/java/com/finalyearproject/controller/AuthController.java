
package com.finalyearproject.controller;


import com.finalyearproject.config.EmailService;
import com.finalyearproject.model.Role.RoleType;
import com.finalyearproject.model.User;
import com.finalyearproject.repository.UserRepository;
import com.finalyearproject.service.PasswordResetTokenService;
import java.util.UUID;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private PasswordResetTokenService passwordResetTokenService;
    
    @Autowired
    private EmailService emailService;

    @PostMapping("/register")
    public String registerUser(@RequestParam String fullName,
                               @RequestParam String email,
                               @RequestParam String universityId,
                               @RequestParam String faculty,
                               @RequestParam String password,
                               @RequestParam String confirmPassword,
                               @RequestParam String role,
                               Model model) {

        // Convert role string to enum
        RoleType selectedRole;
        try {
            selectedRole = RoleType.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", "Lecturer accounts must be created by Admin.");
            model.addAttribute("showRegister", true); 
            return "Login-registration";
        }

        // Validation
        if (selectedRole == RoleType.STUDENT &&
            !universityId.matches("\\d{2}/[A-Z]{2,}/BU/R/\\d+")) {
            model.addAttribute("error", "Invalid Student ID format.");
            model.addAttribute("showRegister", true);
            return "Login-registration";
        }

        if (selectedRole == RoleType.LECTURER) {
            model.addAttribute("error", "Lecturer accounts must be created by Admin.");
            model.addAttribute("showRegister", true);
            return "Login-registration";
        }

        // Check if email or ID already exists
        if (userRepository.existsByEmail(email)) {
            model.addAttribute("error", "Email already registered.");
            model.addAttribute("showRegister", true);
            return "Login-registration";
        }

        if (userRepository.existsByUniversityId(universityId)) {
            model.addAttribute("error", "ID already registered.");
            model.addAttribute("showRegister", true);
            return "Login-registration";
        }
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match. Please try again.");
            model.addAttribute("showRegister", true);
            return "Login-registration";
        }


        // Save user
        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setUniversityId(universityId);
        user.setFaculty(faculty);
        user.setPassword(passwordEncoder.encode(password.trim()));// TODO: Hash this in real system
        user.setRole(selectedRole);

        // Students auto-approved
        if (selectedRole == RoleType.STUDENT) {
            user.setApproved(true);
        }

        userRepository.save(user);

        model.addAttribute("success", "Registration successful. Please login.");
        model.addAttribute("showLogin", true);
        return "Login-registration";
        
    }
    @GetMapping("/")
    public String index(Model model) {
        return showLogin(model);
    }

    @GetMapping("/login")
    public String showLogin(Model model) {
        model.addAttribute("showLogin", true);
        return "Login-registration";
    }

    @GetMapping("/register")
    public String showRegister(Model model) {
        model.addAttribute("showRegister", true);
        return "Login-registration";
    }
    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestParam String email, Model model) {
        Optional<User> user = userRepository.findByEmail(email);
        if(user.isPresent()) {
            User u = user.get();
            String token = UUID.randomUUID().toString();
            passwordResetTokenService.createToken(u, token);
            String link = "https://yourdomain.com/reset-password?token=" + token;
            emailService.sendEmail(u.getEmail(), "Reset Password", "Click link: " + link);
        }
        model.addAttribute("message", "If the email exists, a reset link has been sent");
        model.addAttribute("showLogin", true);
        return "Login-registration";
    }
    @PostMapping("/debug-login")
    public String debugLogin(@RequestParam String username,
                             @RequestParam String password) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        System.out.println("RAW PASSWORD: " + password);
        System.out.println("HASHED PASSWORD: " + user.getPassword());
        boolean matches = passwordEncoder.matches(password, user.getPassword());
        System.out.println("MATCH? " + matches);

        return matches ? "Password matches!" : "Password does NOT match!";
    }
}
