package com.finalyearproject.config;

import com.finalyearproject.model.User;
import com.finalyearproject.model.Role.RoleType;
import com.finalyearproject.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner init(UserRepository userRepository) {
        return args -> {
            // Check if an admin already exists
            if (userRepository.findByRole(RoleType.ADMIN).isEmpty()) {
                User admin = new User();
                admin.setFullName("Super Admin");
                admin.setEmail("admin@university.com");
                admin.setUniversityId("ADMIN001");
                admin.setPassword(new BCryptPasswordEncoder().encode("admin123")); // hash the password
                admin.setRole(RoleType.ADMIN);
                admin.setApproved(true);

                userRepository.save(admin);
                System.out.println("✅ Default admin created: admin@university.com / admin123");
            } else {
                System.out.println("⚡ Admin already exists, skipping initialization");
            }
        };
    }
}