package com.example.budgettracker.controller;

import com.example.budgettracker.service.SessionManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * FR-6: Session Management Controller
 * Provides endpoints for session validation, renewal, and management
 */
@RestController
@RequestMapping("/api/session")
@RequiredArgsConstructor
@Slf4j
public class SessionController {

    private final SessionManagementService sessionManagementService;

    /**
     * Validate current session status
     */
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateSession(
            @RequestHeader("Authorization") String authHeader) {

        String sessionToken = extractSessionToken(authHeader);
        if (sessionToken == null) {
            return ResponseEntity.ok(Map.of(
                    "valid", false,
                    "reason", "No session token provided"
            ));
        }

        boolean isValid = sessionManagementService.isSessionValid(sessionToken);

        if (isValid) {
            return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "message", "Session is valid"
            ));
        } else {
            return ResponseEntity.ok(Map.of(
                    "valid", false,
                    "reason", "Session expired or invalid"
            ));
        }
    }

    /**
     * Manually renew session (heartbeat endpoint)
     */
    @PostMapping("/renew")
    public ResponseEntity<Map<String, Object>> renewSession(
            @RequestHeader("Authorization") String authHeader) {

        String sessionToken = extractSessionToken(authHeader);
        if (sessionToken == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "No session token provided"
            ));
        }

        boolean renewed = sessionManagementService.renewSession(sessionToken);

        if (renewed) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Session renewed successfully"
            ));
        } else {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "Session renewal failed - please re-authenticate"
            ));
        }
    }

    /**
     * Get session status and information
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSessionStatus(
            @RequestHeader("Authorization") String authHeader) {

        String sessionToken = extractSessionToken(authHeader);
        if (sessionToken == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "No session token provided"
            ));
        }

        return sessionManagementService.validateSession(sessionToken)
                .map(session -> {
                    Map<String, Object> responseData = Map.of(
                            "valid", true,
                            "issuedAt", session.getIssuedAt().toString(),
                            "expiresAt", session.getExpiresAt().toString(),
                            "lastActivity", session.getLastActivityAt().toString(),
                            "ipAddress", session.getIpAddress() != null ? session.getIpAddress() : "unknown"
                    );
                    return ResponseEntity.ok(responseData);
                })
                .orElse(ResponseEntity.status(401).body(Map.of(
                        "valid", false,
                        "error", "Invalid or expired session"
                )));
    }

    /**
     * Administrative endpoint to cleanup expired sessions
     */
    @PostMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> cleanupSessions() {
        try {
            sessionManagementService.cleanupExpiredSessions();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Session cleanup completed"
            ));
        } catch (Exception e) {
            log.error("Session cleanup failed: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Session cleanup failed"
            ));
        }
    }

    private String extractSessionToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}