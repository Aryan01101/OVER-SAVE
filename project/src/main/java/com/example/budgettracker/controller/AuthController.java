package com.example.budgettracker.controller;

import com.example.budgettracker.dto.*;
import com.example.budgettracker.model.User;
import com.example.budgettracker.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request) {

        String ipAddress = getClientIpAddress(request);
        log.info("Login attempt for email: {} from IP: {}", loginRequest.getEmail(), ipAddress);

        AuthResponse response = authService.login(loginRequest, ipAddress);

        if (response.getSessionToken() != null) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(
            @Valid @RequestBody SignupRequest signupRequest,
            HttpServletRequest request) {

        String ipAddress = getClientIpAddress(request);
        log.info("Signup attempt for email: {} from IP: {}", signupRequest.getEmail(), ipAddress);

        AuthResponse response = authService.signup(signupRequest, ipAddress);

        if (response.getSessionToken() != null) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/login/idp")
    public ResponseEntity<AuthResponse> loginWithIdp(
            @Valid @RequestBody IdpLoginRequest idpLoginRequest,
            HttpServletRequest request) {

        String ipAddress = getClientIpAddress(request);
        log.info("IdP login attempt for provider: {} from IP: {}", idpLoginRequest.getProvider(), ipAddress);

        AuthResponse response = authService.loginWithIdp(idpLoginRequest, ipAddress);

        if (response.getSessionToken() != null) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader) {
        String sessionToken = extractSessionToken(authHeader);
        if (sessionToken != null) {
            authService.logout(sessionToken);
            log.info("User logged out successfully");
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        String sessionToken = extractSessionToken(authHeader);
        if (sessionToken == null) {
            return ResponseEntity.status(401).build();
        }

        User user = authService.getCurrentUser(sessionToken);
        if (user != null) {
            return ResponseEntity.ok(user);
        } else {
            return ResponseEntity.status(401).build();
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<Boolean> validateSession(@RequestHeader("Authorization") String authHeader) {
        String sessionToken = extractSessionToken(authHeader);
        if (sessionToken == null) {
            return ResponseEntity.ok(false);
        }

        boolean isValid = authService.isValidSession(sessionToken);
        return ResponseEntity.ok(isValid);
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

    private String extractSessionToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}