package com.example.budgettracker.service;

import com.example.budgettracker.dto.PasswordResetRequest;
import com.example.budgettracker.dto.PasswordResetResponse;
import com.example.budgettracker.model.PasswordResetToken;
import com.example.budgettracker.model.User;
import com.example.budgettracker.repository.PasswordResetTokenRepository;
import com.example.budgettracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetServiceImpl implements PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final SessionManagementService sessionManagementService;
    private final PasswordEncoder passwordEncoder;

    private static final int TOKEN_EXPIRY_MINUTES = 15;
    private static final int MAX_TOKENS_PER_USER = 3;
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    @Transactional
    public PasswordResetResponse requestReset(String email) {
        try {
            Optional<User> userOpt = userRepository.findByEmail(email.toLowerCase().trim());

            if (userOpt.isPresent()) {
                User user = userOpt.get();

                // Check if user already has too many valid tokens (prevent spam)
                int validTokenCount = tokenRepository.countValidTokensForUser(user, LocalDateTime.now());
                if (validTokenCount >= MAX_TOKENS_PER_USER) {
                    log.warn("User {} has too many active reset tokens", email);
                    // Still return success to prevent enumeration
                    return PasswordResetResponse.success("If your email is registered, you will receive a password reset link.");
                }

                // Generate secure token
                String token = generateSecureToken();
                LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES);

                // Save token
                PasswordResetToken resetToken = PasswordResetToken.builder()
                        .user(user)
                        .token(token)
                        .expiresAt(expiresAt)
                        .build();

                tokenRepository.save(resetToken);

                // Send email
                emailService.sendPasswordResetEmail(user.getEmail(), user.getFirstName(), token);

                log.info("Password reset requested for user: {}", email);
            } else {
                log.info("Password reset requested for non-existent email: {}", email);
                // Still simulate email sending delay for security
                try {
                    Thread.sleep(100 + RANDOM.nextInt(200)); // 100-300ms delay
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // Always return success to prevent email enumeration
            return PasswordResetResponse.success("If your email is registered, you will receive a password reset link.");

        } catch (Exception e) {
            log.error("Error processing password reset request for email: {}", email, e);
            return PasswordResetResponse.error("An error occurred. Please try again later.");
        }
    }

    @Override
    public PasswordResetResponse validateToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                return PasswordResetResponse.error("Invalid token");
            }

            Optional<PasswordResetToken> tokenOpt = tokenRepository.findValidToken(token.trim(), LocalDateTime.now());

            if (tokenOpt.isEmpty()) {
                log.warn("Invalid or expired token used: {}", token);
                return PasswordResetResponse.error("Invalid or expired reset link. Please request a new one.");
            }

            return PasswordResetResponse.success("Token is valid");

        } catch (Exception e) {
            log.error("Error validating token: {}", token, e);
            return PasswordResetResponse.error("An error occurred. Please try again.");
        }
    }

    @Override
    @Transactional
    public PasswordResetResponse resetPassword(String token, String newPassword) {
        try {
            if (token == null || token.trim().isEmpty()) {
                return PasswordResetResponse.error("Invalid token");
            }

            // Validate password first
            PasswordResetResponse passwordValidation = validatePasswordStrength(newPassword);
            if (!passwordValidation.isSuccess()) {
                return passwordValidation;
            }

            // Find and validate token
            Optional<PasswordResetToken> tokenOpt = tokenRepository.findValidToken(token.trim(), LocalDateTime.now());

            if (tokenOpt.isEmpty()) {
                log.warn("Attempted password reset with invalid token: {}", token);
                return PasswordResetResponse.error("Invalid or expired reset link. Please request a new one.");
            }

            PasswordResetToken resetToken = tokenOpt.get();
            User user = resetToken.getUser();

            // Update password
            user.setHashedPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);

            // Mark token as used
            resetToken.markAsUsed();
            tokenRepository.save(resetToken);

            // Invalidate all other tokens for this user
            tokenRepository.invalidateAllTokensForUser(user);

            // Invalidate all sessions for security
            sessionManagementService.invalidateAllSessionsForUser(user);

            // Send confirmation email
            emailService.sendPasswordChangedEmail(user.getEmail(), user.getFirstName());

            log.info("Password successfully reset for user: {}", user.getEmail());
            return PasswordResetResponse.success("Your password has been successfully reset. Please log in with your new password.");

        } catch (Exception e) {
            log.error("Error resetting password with token: {}", token, e);
            return PasswordResetResponse.error("An error occurred. Please try again.");
        }
    }

    @Override
    public PasswordResetResponse validatePasswordStrength(String password) {
        List<String> errors = new ArrayList<>();

        if (password == null || password.length() < 12) {
            errors.add("Password must be at least 12 characters long");
        }

        if (password != null) {
            if (!Pattern.compile("[A-Z]").matcher(password).find()) {
                errors.add("Password must contain at least one uppercase letter");
            }

            if (!Pattern.compile("[0-9]").matcher(password).find()) {
                errors.add("Password must contain at least one number");
            }

            if (!Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]").matcher(password).find()) {
                errors.add("Password must contain at least one special character");
            }

            // Check for common patterns
            if (password.toLowerCase().contains("password") ||
                password.toLowerCase().contains("123456") ||
                password.toLowerCase().contains("qwerty")) {
                errors.add("Password cannot contain common patterns");
            }
        }

        if (!errors.isEmpty()) {
            return PasswordResetResponse.error("Password does not meet security requirements", errors);
        }

        return PasswordResetResponse.success("Password meets security requirements");
    }

    @Override
    @Transactional
    public int cleanupExpiredTokens() {
        try {
            int deletedCount = tokenRepository.deleteExpiredTokens(LocalDateTime.now());
            if (deletedCount > 0) {
                log.info("Cleaned up {} expired password reset tokens", deletedCount);
            }
            return deletedCount;
        } catch (Exception e) {
            log.error("Error cleaning up expired tokens", e);
            return 0;
        }
    }

    private String generateSecureToken() {
        StringBuilder token = new StringBuilder(64);
        for (int i = 0; i < 64; i++) {
            token.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return token.toString();
    }
}