package com.example.budgettracker.controller;

import com.example.budgettracker.dto.Dashboard.DashboardResponse;
import com.example.budgettracker.dto.Dashboard.FinancialAggregatesResponse;
import com.example.budgettracker.dto.Dashboard.SpendingTrendData;
import com.example.budgettracker.model.User;
import com.example.budgettracker.service.AuthService;
import com.example.budgettracker.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * FR-14: Dashboard Controller
 * Provides REST endpoints for dashboard data
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;
    private final AuthService authService;

    /**
     * FR-14: Get complete dashboard data
     * GET /api/dashboard
     */
    @GetMapping
    public ResponseEntity<?> getDashboardData(@RequestHeader("Authorization") String authHeader) {
        try {
            log.debug("🔐 FR-14: Received Authorization header: {}", authHeader != null ? authHeader.substring(0, Math.min(20, authHeader.length())) + "..." : "NULL");

            Long userId = getUserIdFromToken(authHeader);

            log.debug("🔐 FR-14: Extracted userId: {}", userId);

            if (userId == null) {
                log.warn("⚠️ FR-14: User not authenticated - Authorization header: {}",
                         authHeader != null ? authHeader.substring(0, Math.min(20, authHeader.length())) + "..." : "NULL");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("User not authenticated"));
            }

            log.info("FR-14: Fetching dashboard data for user {}", userId);
            DashboardResponse response = dashboardService.getDashboardData(userId);

            // FR-14: Handle data unavailability
            if (!response.isDataAvailable()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(response);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("FR-14: Error in getDashboardData", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(createErrorResponse("Failed to load dashboard data. Please try again."));
        }
    }

    /**
     * FR-14: Get financial aggregates only
     * GET /api/dashboard/financial-aggregates
     */
    @GetMapping("/financial-aggregates")
    public ResponseEntity<?> getFinancialAggregates(@RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("User not authenticated"));
            }

            log.info("FR-14: Fetching financial aggregates for user {}", userId);
            FinancialAggregatesResponse response = dashboardService.getFinancialAggregates(userId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("FR-14: Error in getFinancialAggregates", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(createErrorResponse("Failed to load financial data."));
        }
    }

    /**
     * FR-14: Get spending trend data for charts
     * GET /api/dashboard/spending-trend?period=week
     * @param period "WEEK", "MONTH", or "YEAR"
     */
    @GetMapping("/spending-trend")
    public ResponseEntity<?> getSpendingTrend(
            @RequestParam(defaultValue = "WEEK") String period,
            @RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("User not authenticated"));
            }

            log.info("FR-14: Fetching spending trend for user {} with period {}", userId, period);
            SpendingTrendData response = dashboardService.getSpendingTrend(userId, period);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("FR-14: Error in getSpendingTrend", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(createErrorResponse("Failed to load spending trend."));
        }
    }

    /**
     * Extract userId from session token
     */
    private Long getUserIdFromToken(String authHeader) {
        String sessionToken = extractSessionToken(authHeader);
        log.debug("🔐 FR-14: Extracted session token: {}", sessionToken != null ? sessionToken.substring(0, Math.min(10, sessionToken.length())) + "..." : "NULL");

        if (sessionToken == null) {
            log.warn("⚠️ FR-14: No session token found in Authorization header");
            return null;
        }

        User user = authService.getCurrentUser(sessionToken);
        log.debug("🔐 FR-14: Retrieved user from session: {}", user != null ? user.getEmail() : "NULL");

        return user != null ? user.getUserId() : null;
    }

    /**
     * Extract session token from Authorization header
     */
    private String extractSessionToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    /**
     * FR-14: Helper method to create error response with fallback message
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("dataAvailable", false);
        error.put("message", message);
        error.put("timestamp", System.currentTimeMillis());
        return error;
    }
}
