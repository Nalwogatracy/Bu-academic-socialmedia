package com.finalyearproject.config;

import com.finalyearproject.model.User;
import com.finalyearproject.repository.UserRepository;
import com.finalyearproject.service.OAuth2UserService;
import com.finalyearproject.service.UserService;
import com.finalyearproject.service.UserStatusService;
import java.util.Collections;
import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.*;

@Configuration
public class SecurityConfig {

    private final DataSource dataSource;
    private final OAuth2UserService oAuth2UserService;
    private final UserStatusService userStatusService;
    private final UserService userService;

    public SecurityConfig(DataSource dataSource, OAuth2UserService oAuth2UserService,UserStatusService userStatusService,UserService userService) {
        this.dataSource = dataSource;
        this.oAuth2UserService = oAuth2UserService;
        this.userStatusService = userStatusService;
        this.userService = userService;
    }

    // Password Encoder
   /* @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    } */
    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
                                                       PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService); // ✅ pass UserDetailsService
        
        authProvider.setPasswordEncoder(passwordEncoder);                                          // set encoder

        return new ProviderManager(authProvider);
    }

    // Authentication Logic is delegated to CustomUserDetailsService

    // Remember Me Token Repository
    @Bean
    public PersistentTokenRepository persistentTokenRepository() {
        JdbcTokenRepositoryImpl repo = new JdbcTokenRepositoryImpl();
        repo.setDataSource(dataSource);
        repo.setCreateTableOnStartup(false); // ← ADD THIS LINE
        return repo;
    }

    // Security Filter
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, UserDetailsService userDetailsService) throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/register", "/forgot-password", "/reset-password",
                        "/css/**", "/js/**", "/images/**", "/webfonts/**", "/fonts/**",
                        "/user/profile-picture/**", "/sw.js", "/offline.html", "/manifest.json",
                        "/icons/**", "/actuator/health").permitAll()
                .requestMatchers("/admin/**", "/actuator/**").hasRole("ADMIN")
                .requestMatchers("/lecturer/**").hasRole("LECTURER")
                .requestMatchers("/student/**").hasRole("STUDENT")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler((request, response, authentication) -> {
                    String role = authentication.getAuthorities()
                            .iterator().next().getAuthority();
                    if (role.equals("ROLE_ADMIN")) response.sendRedirect("/admin/dashboard");
                    else if (role.equals("ROLE_LECTURER")) response.sendRedirect("/lecturer/dashboard");
                    else response.sendRedirect("/student/dashboard");
                })
                .failureHandler((request, response, exception) -> {
                    response.sendRedirect("/login?error=true");
                })
                .permitAll()
            )
            .oauth2Login(oauth2 -> oauth2
            .loginPage("/login")
            .userInfoEndpoint(userInfo -> userInfo
                .userService(oAuth2UserService)  // ← wire your service
            )
            .successHandler((request, response, authentication) -> {
                response.sendRedirect("/student/dashboard");
            })
        )
            .logout(logout -> logout
                    .addLogoutHandler((request, response, authentication) -> {
                if (authentication != null) {
                    User user = userService.findByEmail(authentication.getName());
                    if (user != null) {
                        userStatusService.markOffline(user);
                    }
                }
            })
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            .rememberMe(remember -> remember
                .key("bugema-remember-me")
                .tokenValiditySeconds(7 * 24 * 60 * 60)
                .tokenRepository(persistentTokenRepository())
                .userDetailsService(userDetailsService)  // <-- MUST add this
            );

        return http.build();
    }
}