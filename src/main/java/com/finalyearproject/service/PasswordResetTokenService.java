
package com.finalyearproject.service;

import com.finalyearproject.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;


@Service
public class PasswordResetTokenService {

    // Simple in-memory token store (for demo purposes)
    private Map<String, TokenData> tokens = new HashMap<>();

    public void createToken(User user, String token) {
        tokens.put(token, new TokenData(user, LocalDateTime.now().plusHours(1))); // token valid for 1 hour
    }

    public User validateToken(String token) {
        TokenData data = tokens.get(token);
        if (data != null && data.expiry.isAfter(LocalDateTime.now())) {
            return data.user;
        }
        return null;
    }

    private static class TokenData {
        User user;
        LocalDateTime expiry;
        TokenData(User user, LocalDateTime expiry) {
            this.user = user;
            this.expiry = expiry;
        }
    }
}
