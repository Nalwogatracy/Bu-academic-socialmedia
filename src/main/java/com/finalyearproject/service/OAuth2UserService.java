package com.finalyearproject.service;

import com.finalyearproject.model.Role.RoleType;
import com.finalyearproject.model.User;
import com.finalyearproject.repository.UserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class OAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    public OAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String email = oAuth2User.getAttribute("email");
        String name  = oAuth2User.getAttribute("name");

        // If user doesn't exist in DB yet, auto-create them as STUDENT
        if (email != null && !userRepository.existsByEmail(email)) {
            User newUser = new User();
            newUser.setFullName(name != null ? name : "OAuth2 User");
            newUser.setEmail(email);
            newUser.setUniversityId("OAUTH-" + System.currentTimeMillis());
            newUser.setPassword("OAUTH2_NO_PASSWORD");
            newUser.setRole(RoleType.STUDENT);
            newUser.setFaculty("Unknown");
            newUser.setApproved(true);
            userRepository.save(newUser);
        }

        return oAuth2User;
    }
}