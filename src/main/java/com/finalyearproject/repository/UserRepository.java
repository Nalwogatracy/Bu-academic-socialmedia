
package com.finalyearproject.repository;

import com.finalyearproject.model.Role.RoleType;
import com.finalyearproject.model.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    boolean existsByUniversityId(String universityId);
    
    Optional<User> findByEmail(String email);

    Optional<User> findByUniversityId(String universityId);
    List<User> findByApprovedFalse();

    // Find admin user (assuming 1 admin for now)
    Optional<User> findByRole(RoleType role);
    List<User> findByApprovedFalseAndRole(RoleType role);
    
    @Query("SELECT u FROM User u WHERE LOWER(u.fullName) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(u.universityId) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<User> search(@Param("q") String q);
    
}
