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
        com.finalyearproject.model.User user = userRepository.findByEmail(username)
            .orElseGet(() -> userRepository.findByUniversityId(username)
                .orElseThrow(() ->
                    new UsernameNotFoundException("User not found: " + username)));

        if (!user.isApproved()) {
            throw new DisabledException("Account is not approved yet.");
        }
        if (user.getRole().name().equals("LECTURER") && !user.isApproved()) {
            throw new DisabledException("Lecturer account not approved yet");
        }

        return new org.springframework.security.core.userdetails.User(
                username,
                user.getPassword(),
                Collections.singleton(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}
