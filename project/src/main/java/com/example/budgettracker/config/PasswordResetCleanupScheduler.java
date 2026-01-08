package com.example.budgettracker.config;

import com.example.budgettracker.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * FR-05: Password Reset Token Cleanup Scheduler
 * Automatically cleans up expired tokens every hour
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PasswordResetCleanupScheduler {

    private final PasswordResetService passwordResetService;

    /**
     * Clean up expired password reset tokens every hour
     * Runs at the top of every hour (0 minutes, 0 seconds)
     */
    @Scheduled(cron = "0 0 * * * *")
    public void cleanupExpiredTokens() {
        try {
            int deletedCount = passwordResetService.cleanupExpiredTokens();
            if (deletedCount > 0) {
                log.info("Password reset cleanup: Removed {} expired tokens", deletedCount);
            } else {
                log.debug("Password reset cleanup: No expired tokens to remove");
            }
        } catch (Exception e) {
            log.error("Error during password reset token cleanup", e);
        }
    }

    /**
     * Additional cleanup at startup to clear any expired tokens
     * Runs 30 seconds after application starts
     */
    @Scheduled(initialDelay = 30000, fixedDelay = Long.MAX_VALUE)
    public void initialCleanup() {
        try {
            int deletedCount = passwordResetService.cleanupExpiredTokens();
            log.info("Initial password reset cleanup: Removed {} expired tokens", deletedCount);
        } catch (Exception e) {
            log.error("Error during initial password reset token cleanup", e);
        }
    }
}