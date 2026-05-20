package com.finalyearproject.service;

import com.finalyearproject.model.PasswordResetToken;
import com.finalyearproject.model.User;
import com.finalyearproject.repository.PasswordResetTokenRepository;
import com.finalyearproject.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class PasswordResetTokenService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;

    public PasswordResetTokenService(PasswordResetTokenRepository tokenRepository,
                                     UserRepository userRepository) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void createToken(User user, String token) {
        PasswordResetToken resetToken = new PasswordResetToken(
                token, user.getEmail(), LocalDateTime.now().plusHours(1));
        tokenRepository.save(resetToken);
    }

    @Transactional
    public User validateToken(String token) {
        Optional<PasswordResetToken> opt = tokenRepository.findByToken(token);
        if (opt.isPresent()) {
            PasswordResetToken resetToken = opt.get();
            if (resetToken.isValid()) {
                resetToken.setUsed(true);
                tokenRepository.save(resetToken);
                return userRepository.findByEmail(resetToken.getEmail()).orElse(null);
            }
        }
        return null;
    }
}
