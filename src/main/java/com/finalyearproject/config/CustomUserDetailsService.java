
package com.finalyearproject.config;


import com.finalyearproject.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import java.util.Collections;
import org.springframework.security.authentication.DisabledException;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // username can be email or studentId
        com.finalyearproject.model.User user = userRepository.findByEmail(username)
            .orElseGet(() -> userRepository.findByUniversityId(username)
                .orElseThrow(() ->
                    new UsernameNotFoundException("User not found: " + username)));

            System.out.println("LOGIN ATTEMPT: " + username);
            System.out.println("PASSWORD IN DB: " + user.getPassword());
            System.out.println("ROLE: " + user.getRole());
            System.out.println("APPROVED: " + user.isApproved());


        System.out.println("USERNAME ATTEMPT: " + username);
        System.out.println("STORED HASHED PASSWORD: " + user.getPassword());
        if (!user.isApproved()) {
            throw new DisabledException("Account is not approved yet.");
        }
        if (user.getRole().name().equals("LECTURER") && !user.isApproved()) {
            throw new DisabledException("Lecturer account not approved yet");
        }

        System.out.println("EMAIL: " + user.getEmail());
        System.out.println("PASSWORD IN DB: " + user.getPassword());

        return new org.springframework.security.core.userdetails.User(                
                username,
                user.getPassword(),
                Collections.singleton(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}