package com.example.budgettracker.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SessionMigrationRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Running session table migration...");

        try {
            // Update existing sessions to have default values for new fields
            String updateExistingSessions = """
                UPDATE session
                SET is_active = COALESCE(is_active, true),
                    last_activity_at = COALESCE(last_activity_at, issued_at),
                    token_signature = COALESCE(token_signature, 'migration_placeholder')
                WHERE is_active IS NULL OR last_activity_at IS NULL OR token_signature IS NULL
                """;

            int updatedRows = jdbcTemplate.update(updateExistingSessions);
            log.info("Updated {} existing session records with default values", updatedRows);

            // Invalidate sessions without proper signatures (they're from before FR-6)
            String invalidateOldSessions = """
                UPDATE session
                SET is_active = false
                WHERE token_signature = 'migration_placeholder'
                """;

            int invalidatedRows = jdbcTemplate.update(invalidateOldSessions);
            log.info("Invalidated {} old sessions without proper signatures", invalidatedRows);

            log.info("Session table migration completed successfully");

        } catch (Exception e) {
            log.error("Session migration failed: {}", e.getMessage());
            // Don't fail startup, just log the error
        }
    }
}