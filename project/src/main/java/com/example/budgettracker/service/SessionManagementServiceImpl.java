package com.example.budgettracker.service;

import com.example.budgettracker.model.Session;
import com.example.budgettracker.model.User;
import com.example.budgettracker.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * FR-6: Session Management Implementation
 * Secure session management with 5-minute idle timeout and tampering detection
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionManagementServiceImpl implements SessionManagementService {

    private final SessionRepository sessionRepository;

    @Value("${app.session.secret:defaultSecretChangeInProduction}")
    private String sessionSecret;

    private static final int SESSION_DURATION_HOURS = 24;
    private static final int IDLE_TIMEOUT_MINUTES = 5;
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    @Override
    @Transactional
    public Session createSession(User user, String ipAddress, String userAgent) {
        try {
            LocalDateTime now = LocalDateTime.now();

            // Invalidate existing sessions for this user
            sessionRepository.findActiveSessionsByUserId(user.getUserId())
                    .forEach(existingSession -> {
                        existingSession.setIsActive(false);
                        sessionRepository.save(existingSession);
                    });

            // Generate secure token
            String token = generateSecureToken(user, now);
            String signature = generateTokenSignature(token);

            Session session = Session.builder()
                    .user(user)
                    .sessionToken(token)
                    .tokenSignature(signature)
                    .issuedAt(now)
                    .expiresAt(now.plusHours(SESSION_DURATION_HOURS))
                    .lastActivityAt(now)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .isActive(true)
                    .build();

            session = sessionRepository.save(session);
            log.info("Created new session for user {} from IP {}", user.getEmail(), ipAddress);

            return session;

        } catch (Exception e) {
            log.error("Failed to create session for user {}: {}", user.getEmail(), e.getMessage());
            throw new RuntimeException("Session creation failed", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Session> validateSession(String token) {
        if (token == null || token.trim().isEmpty()) {
            return Optional.empty();
        }

        Optional<Session> sessionOpt = sessionRepository.findBySessionToken(token);

        if (sessionOpt.isEmpty()) {
            log.warn("Session not found for token: {}", maskToken(token));
            return Optional.empty();
        }

        Session session = sessionOpt.get();

        // Check if session is active
        if (!session.getIsActive()) {
            log.warn("Session is inactive: {}", maskToken(token));
            return Optional.empty();
        }

        // Check if session is expired
        if (session.isExpired()) {
            log.warn("Session expired: {}", maskToken(token));
            invalidateSession(token);
            return Optional.empty();
        }

        // Check if session is idle (5 minutes)
        if (session.isIdle()) {
            log.warn("Session idle timeout: {}", maskToken(token));
            invalidateSession(token);
            return Optional.empty();
        }

        // Verify token signature for tampering detection
        if (!verifyTokenSignature(token, session.getTokenSignature())) {
            log.error("Token tampering detected for session: {}", maskToken(token));
            invalidateSession(token);
            return Optional.empty();
        }

        return Optional.of(session);
    }

    @Override
    @Transactional
    public boolean renewSession(String token) {
        try {
            Optional<Session> sessionOpt = validateSession(token);

            if (sessionOpt.isEmpty()) {
                return false;
            }

            Session session = sessionOpt.get();
            session.updateActivity();

            // Extend expiration if close to expiry (within 1 hour)
            LocalDateTime now = LocalDateTime.now();
            if (session.getExpiresAt().isBefore(now.plusHours(1))) {
                session.setExpiresAt(now.plusHours(SESSION_DURATION_HOURS));
                log.debug("Extended session expiration for user {}", session.getUser().getEmail());
            }

            sessionRepository.save(session);
            log.debug("Renewed session activity for user {}", session.getUser().getEmail());

            return true;

        } catch (Exception e) {
            log.error("Failed to renew session {}: {}", maskToken(token), e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional
    public void invalidateSession(String token) {
        try {
            sessionRepository.findBySessionToken(token)
                    .ifPresent(session -> {
                        session.setIsActive(false);
                        sessionRepository.save(session);
                        log.info("Invalidated session for user {}", session.getUser().getEmail());
                    });
        } catch (Exception e) {
            log.error("Failed to invalidate session {}: {}", maskToken(token), e.getMessage());
        }
    }

    @Override
    @Transactional
    public void invalidateAllSessionsForUser(User user) {
        try {
            sessionRepository.findActiveSessionsByUserId(user.getUserId())
                    .forEach(session -> {
                        session.setIsActive(false);
                        sessionRepository.save(session);
                    });
            log.info("Invalidated all sessions for user {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to invalidate sessions for user {}: {}", user.getEmail(), e.getMessage());
        }
    }

    @Override
    @Transactional
    public void cleanupExpiredSessions() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime idleCutoff = now.minusMinutes(IDLE_TIMEOUT_MINUTES);

            List<Session> expiredSessions = sessionRepository.findExpiredSessions(now, idleCutoff);

            for (Session session : expiredSessions) {
                session.setIsActive(false);
                sessionRepository.save(session);
            }

            if (!expiredSessions.isEmpty()) {
                log.info("Cleaned up {} expired sessions", expiredSessions.size());
            }

        } catch (Exception e) {
            log.error("Failed to cleanup expired sessions: {}", e.getMessage());
        }
    }

    @Override
    public boolean isSessionValid(String token) {
        return validateSession(token).isPresent();
    }

    @Override
    public Optional<User> getUserFromSession(String token) {
        Optional<Session> sessionOpt = validateSession(token);

        if (sessionOpt.isPresent()) {
            // Auto-renew session to prevent idle timeout on every authenticated request
            renewSession(token);
            return Optional.of(sessionOpt.get().getUser());
        }

        return Optional.empty();
    }

    @Override
    public String generateSecureToken(User user, LocalDateTime issuedAt) {
        try {
            SecureRandom random = new SecureRandom();
            byte[] tokenBytes = new byte[32];
            random.nextBytes(tokenBytes);

            String baseToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

            // Add user and timestamp info for additional entropy
            String tokenData = String.format("%s:%s:%s",
                    baseToken,
                    user.getUserId(),
                    issuedAt.toString());

            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(tokenData.getBytes(StandardCharsets.UTF_8));

        } catch (Exception e) {
            log.error("Failed to generate secure token: {}", e.getMessage());
            throw new RuntimeException("Token generation failed", e);
        }
    }

    @Override
    public boolean verifyTokenSignature(String token, String signature) {
        try {
            String expectedSignature = generateTokenSignature(token);
            return expectedSignature.equals(signature);
        } catch (Exception e) {
            log.error("Failed to verify token signature: {}", e.getMessage());
            return false;
        }
    }

    private String generateTokenSignature(String token) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(sessionSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(keySpec);

            byte[] signature = mac.doFinal(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature);

        } catch (Exception e) {
            log.error("Failed to generate token signature: {}", e.getMessage());
            throw new RuntimeException("Signature generation failed", e);
        }
    }

    private String maskToken(String token) {
        if (token == null || token.length() < 8) {
            return "***";
        }
        return token.substring(0, 4) + "***" + token.substring(token.length() - 4);
    }
}