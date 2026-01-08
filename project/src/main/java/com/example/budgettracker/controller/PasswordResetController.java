package com.example.budgettracker.controller;

import com.example.budgettracker.dto.PasswordResetRequest;
import com.example.budgettracker.dto.PasswordResetResponse;
import com.example.budgettracker.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * FR-05: Password Reset Controller
 * Handles password reset requests with 15-minute token expiry
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    /**
     * POST /api/auth/forgot-password
     * Request password reset - always returns success to prevent email enumeration
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<PasswordResetResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = getClientIpAddress(httpRequest);
        log.info("Password reset requested for email: {} from IP: {}", request.getEmail(), ipAddress);

        PasswordResetResponse response = passwordResetService.requestReset(request.getEmail());
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/auth/reset-password/:token
     * Validate reset token (for frontend to check before showing reset form)
     */
    @GetMapping("/reset-password/{token}")
    public ResponseEntity<PasswordResetResponse> validateResetToken(@PathVariable String token) {
        log.info("Token validation requested for token: {}", maskToken(token));

        PasswordResetResponse response = passwordResetService.validateToken(token);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * POST /api/auth/reset-password
     * Reset password using token
     */
    @PostMapping("/reset-password")
    public ResponseEntity<PasswordResetResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = getClientIpAddress(httpRequest);
        log.info("Password reset attempt for token: {} from IP: {}", maskToken(request.getToken()), ipAddress);

        PasswordResetResponse response = passwordResetService.resetPassword(
                request.getToken(),
                request.getNewPassword()
        );

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * POST /api/auth/validate-password
     * Validate password strength (for frontend validation)
     */
    @PostMapping("/validate-password")
    public ResponseEntity<PasswordResetResponse> validatePassword(@RequestBody Map<String, String> request) {
        String password = request.get("password");

        PasswordResetResponse response = passwordResetService.validatePasswordStrength(password);
        return ResponseEntity.ok(response);
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    private String maskToken(String token) {
        if (token == null || token.length() < 8) {
            return "***";
        }
        return token.substring(0, 4) + "***" + token.substring(token.length() - 4);
    }

    // Inner DTOs for this controller
    public static class ForgotPasswordRequest {
        @Email(message = "Please provide a valid email address")
        @NotBlank(message = "Email is required")
        private String email;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    public static class ResetPasswordRequest {
        @NotBlank(message = "Token is required")
        private String token;

        @NotBlank(message = "New password is required")
        private String newPassword;

        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }
}