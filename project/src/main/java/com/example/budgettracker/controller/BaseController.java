package com.example.budgettracker.controller;

import com.example.budgettracker.model.User;
import com.example.budgettracker.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Base controller providing common authentication utilities
 * All controllers should extend this to ensure consistent Bearer token authentication
 */
@Slf4j
public abstract class BaseController {

    @Autowired
    protected AuthService authService;

    /**
     * Extract userId from Bearer token in Authorization header
     * @param authHeader Authorization header with "Bearer {token}" format
     * @return userId of the authenticated user
     * @throws IllegalArgumentException if token is missing or invalid
     */
    protected Long getUserIdFromToken(String authHeader) {
        String sessionToken = extractSessionToken(authHeader);
        log.debug("🔐 Extracted session token: {}", sessionToken != null ? sessionToken.substring(0, Math.min(10, sessionToken.length())) + "..." : "NULL");

        if (sessionToken == null) {
            log.warn("⚠️ No session token found in Authorization header");
            throw new IllegalArgumentException("Missing or invalid Authorization header");
        }

        User user = authService.getCurrentUser(sessionToken);
        log.debug("🔐 Retrieved user from session: {}", user != null ? user.getEmail() : "NULL");

        if (user == null) {
            log.warn("⚠️ No user found for session token");
            throw new IllegalArgumentException("Invalid session token");
        }

        return user.getUserId();
    }

    /**
     * Extract session token from Authorization header
     * @param authHeader Authorization header string
     * @return session token without "Bearer " prefix, or null if invalid
     */
    private String extractSessionToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
