package com.example.budgettracker.service;

import com.example.budgettracker.model.Session;
import com.example.budgettracker.model.User;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * FR-6: Session Management Service Interface
 * Manages secure session tokens with 5-minute idle timeout
 */
public interface SessionManagementService {

    /**
     * Create a new session with secure token
     */
    Session createSession(User user, String ipAddress, String userAgent);

    /**
     * Validate session token and check for tampering
     */
    Optional<Session> validateSession(String token);

    /**
     * Renew session on activity (reset idle timeout)
     */
    boolean renewSession(String token);

    /**
     * Invalidate session (logout)
     */
    void invalidateSession(String token);

    /**
     * Invalidate all sessions for a specific user (used for password reset)
     */
    void invalidateAllSessionsForUser(User user);

    /**
     * Clean up expired and idle sessions
     */
    void cleanupExpiredSessions();

    /**
     * Check if session is valid (not expired, not idle, not tampered)
     */
    boolean isSessionValid(String token);

    /**
     * Get user from valid session
     */
    Optional<User> getUserFromSession(String token);

    /**
     * Generate secure signed token
     */
    String generateSecureToken(User user, LocalDateTime issuedAt);

    /**
     * Verify token signature
     */
    boolean verifyTokenSignature(String token, String signature);
}