package com.example.budgettracker.controller;

import com.example.budgettracker.dto.Goal.GoalRequest;
import com.example.budgettracker.dto.Goal.GoalResponse;
import com.example.budgettracker.dto.Goal.ContributionRequest;
import com.example.budgettracker.dto.Goal.ContributionResponse;
import com.example.budgettracker.model.CashFlow;
import com.example.budgettracker.service.GoalService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.LocalDate;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GoalController
 */
@Validated
@RestController
@RequestMapping("/api/goals")
public class GoalController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(GoalController.class);

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    // Helper for extracting user ID from Authorization header
    private Long getUserId(String authHeader) {
        return getUserIdFromToken(authHeader);
    }

    // ===== Create Goal =====
    @PostMapping
    public ResponseEntity<GoalResponse> createGoal(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody GoalRequest request) {

        Long userId = getUserId(authHeader);
        GoalResponse created = goalService.createGoal(userId, request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

    // ===== Get All Goals =====
    @GetMapping
    public ResponseEntity<?> getAllGoals(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        Long userId = null;

        // Try token first
        if (authHeader != null && !authHeader.isBlank()) {
            try {
                userId = getUserId(authHeader);
            } catch (RuntimeException ex) {
                // invalid token -> respond 401
                logger.warn("Invalid Authorization token provided: {}", ex.getMessage());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid Authorization token"));
            }
        }

        // If no token, try session (JSESSIONID)
        if (userId == null) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                Object uidObj = session.getAttribute("userId"); // adjust key based on your session usage
                if (uidObj instanceof Long) {
                    userId = (Long) uidObj;
                } else if (uidObj instanceof Integer) {
                    userId = ((Integer) uidObj).longValue();
                }
            }
        }

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));
        }

        List<GoalResponse> goals = goalService.getAllGoals(userId);
        return ResponseEntity.ok(goals);
    }

    // ===== Get Single Goal =====
    @GetMapping("/{id}")
    public ResponseEntity<GoalResponse> getGoal(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {

        Long userId = getUserId(authHeader);
        return ResponseEntity.ok(goalService.getGoalById(userId, id));
    }

    // ===== Update Goal (Partial) =====
    @PatchMapping("/{id}")
    public ResponseEntity<GoalResponse> updateGoal(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @RequestBody GoalRequest request) {

        Long userId = getUserId(authHeader);
        return ResponseEntity.ok(goalService.updateGoal(userId, id, request));
    }

    // ===== Delete Goal =====
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGoal(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {

        Long userId = getUserId(authHeader);
        goalService.deleteGoal(userId, id);
        return ResponseEntity.noContent().build();
    }

    // ===== Contribute to Goal =====
    @PostMapping("/contribute")
    public ResponseEntity<ContributionResponse> contribute(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody ContributionRequest request) {

        Long userId = getUserId(authHeader);
        ContributionResponse resp = goalService.contributeToGoal(userId, request);
        return ResponseEntity.ok(resp);
    }

    // ===== Get Contributions =====
    @GetMapping("/{id}/contributions")
    public ResponseEntity<List<CashFlow>> getContributions(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {

        Long userId = getUserId(authHeader);
        return ResponseEntity.ok(goalService.getContributions(userId, id, from, to));
    }

    // ---------- Temporary debug endpoint (local use only) ----------
    // Remove this before deploying to prod
    @GetMapping("/debug/session-info")
    public ResponseEntity<Map<String, Object>> debugSessionInfo(HttpServletRequest request) {
        Map<String, Object> out = new HashMap<>();

        HttpSession session = request.getSession(false);
        if (session == null) {
            out.put("sessionPresent", false);
        } else {
            out.put("sessionPresent", true);
            out.put("sessionId", session.getId());

            Map<String, Object> attrs = new HashMap<>();
            Enumeration<String> names = session.getAttributeNames();
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                Object value = session.getAttribute(name);
                attrs.put(name, value != null ? value.toString() : null);
            }
            out.put("sessionAttributes", attrs);
        }

        // SecurityContext info (if Spring Security is in use)
        try {
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth == null) {
                out.put("securityContext", "null");
            } else {
                Map<String, Object> sc = new HashMap<>();
                sc.put("authenticated", auth.isAuthenticated());
                sc.put("principalClass", auth.getPrincipal() != null ? auth.getPrincipal().getClass().getName() : null);
                sc.put("principalToString", auth.getPrincipal() != null ? auth.getPrincipal().toString() : null);
                sc.put("authorities", auth.getAuthorities().toString());
                out.put("securityContext", sc);
            }
        } catch (Exception e) {
            out.put("securityContext", "error: " + e.getMessage());
        }

        return ResponseEntity.ok(out);
    }
}
