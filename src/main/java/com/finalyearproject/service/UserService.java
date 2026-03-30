package com.finalyearproject.service;

import com.finalyearproject.model.Role.RoleType;
import com.finalyearproject.model.User;
import com.finalyearproject.model.UserStatus;
import com.finalyearproject.repository.UserRepository;
import com.finalyearproject.repository.UserStatusRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    

    public UserService(UserRepository userRepository ) {
        this.userRepository = userRepository;
        
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public int calculateEngagement(User user) {
        if (user.getMaterials() == null || user.getMaterials().isEmpty()) return 0;
        long completed = user.getMaterials().stream().filter(m -> m.isCompleted()).count();
        return (int) ((completed * 100) / user.getMaterials().size());
    }
    

    public int countAllUsers() {
        return 1234;
    }

    public int countActiveUsers() {
        return 892;
    }

    public int countNewUsersToday() {
        return 12;
    }

    public int countStudents() {
        return 1148;
    }

    public int countLecturers() {
        return 86;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    public List<User> getPendingUsers() {
        return userRepository.findByApprovedFalse();
    }
    public User getLoggedInAdmin() {
        // This assumes you have a security context or a single admin
        Optional<User> admin = userRepository.findByRole(RoleType.ADMIN);
        return admin.orElse(null); // null if no admin exists
    }
    public void approveUser(Long id){
        User user = userRepository.findById(id).orElseThrow();
        user.setApproved(true);
        userRepository.save(user);
    }

    // Reject user by ID
    public void rejectUser(Long id){
        userRepository.deleteById(id);
    }
    // Create new user
    public void createUser(User user){
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        //user.setApproved(false); // default pending
        if (user.getRole() == RoleType.LECTURER) {
            user.setApproved(false); // lecturers wait for admin approval
        } else {
            user.setApproved(true);  // students and admins get instant access
        }
        userRepository.save(user);
    }

    // Example: user statistics
    public int getUserStatistics(){
        return (int) userRepository.count(); // total users
    }
    public User updateUser(User user) {
        // Optional: fetch existing user first if you want partial updates
        User existingUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        existingUser.setFullName(user.getFullName());
        existingUser.setEmail(user.getEmail());
        existingUser.setUniversityId(user.getUniversityId());
        existingUser.setRole(user.getRole());
        existingUser.setFaculty(user.getFaculty());
        //existingUser.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            existingUser.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        existingUser.setApproved(user.isApproved());

        return userRepository.save(existingUser);
    }
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public void suspendUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id " + id));
        user.setApproved(false); // or set status to "SUSPENDED" if you have a status field
        userRepository.save(user);
    }
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
    public String generateTemporaryPassword() {
        // Simple example: 8-char random alphanumeric
        return UUID.randomUUID().toString().substring(0, 8);
    }

    public void updatePassword(User user, String rawPassword) {
        // Hash password before saving
        user.setPassword(passwordEncoder.encode(rawPassword));
        createUser(user); // or updateUser(user)
    }

    public void sendPasswordResetEmail(User user, String tempPassword) {
        // Optional: implement email sending here
        // e.g., emailService.send(user.getEmail(), "Your new password", "Password: " + tempPassword);
    }
    public User saveUser(User user) {
        return userRepository.save(user);
    }
    public List<User> getAllUsersExcept(User currentUser) {
    return userRepository.findAll()
            .stream()
            .filter(u -> !u.getId().equals(currentUser.getId()))
            .toList();
}
}
