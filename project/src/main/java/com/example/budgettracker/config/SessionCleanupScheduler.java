package com.example.budgettracker.config;

import com.example.budgettracker.service.SessionManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * FR-6: Session Cleanup Scheduler
 * Automatically cleans up expired and idle sessions every 5 minutes
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SessionCleanupScheduler {

    private final SessionManagementService sessionManagementService;

    /**
     * Clean up expired and idle sessions every 5 minutes
     * This ensures the 5-minute idle timeout is enforced
     */
    @Scheduled(fixedRate = 5 * 60 * 1000) // 5 minutes in milliseconds
    public void cleanupExpiredSessions() {
        try {
            log.debug("Starting scheduled session cleanup");
            sessionManagementService.cleanupExpiredSessions();
            log.debug("Completed scheduled session cleanup");
        } catch (Exception e) {
            log.error("Failed to cleanup expired sessions: {}", e.getMessage(), e);
        }
    }

    /**
     * More aggressive cleanup every hour to remove old inactive sessions
     */
    @Scheduled(fixedRate = 60 * 60 * 1000) // 1 hour in milliseconds
    public void hourlySessionCleanup() {
        try {
            log.info("Starting hourly session cleanup");
            sessionManagementService.cleanupExpiredSessions();
            log.info("Completed hourly session cleanup");
        } catch (Exception e) {
            log.error("Failed to perform hourly session cleanup: {}", e.getMessage(), e);
        }
    }
}