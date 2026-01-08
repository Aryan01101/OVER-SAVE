package com.example.budgettracker.config;

import com.example.budgettracker.service.SessionManagementService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * FR-6: Session Activity Filter
 * Automatically renews session on user activity and enforces 5-minute idle timeout
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class SessionActivityFilter implements Filter {

    private final SessionManagementService sessionManagementService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Skip filter for auth endpoints and static resources
        String requestPath = httpRequest.getRequestURI();
        if (shouldSkipFilter(requestPath)) {
            chain.doFilter(request, response);
            return;
        }

        // Extract session token from Authorization header
        String authHeader = httpRequest.getHeader("Authorization");
        String sessionToken = extractSessionToken(authHeader);

        // If no token, continue without session validation for public endpoints
        if (sessionToken == null) {
            chain.doFilter(request, response);
            return;
        }

        // Validate and renew session
        boolean sessionValid = sessionManagementService.isSessionValid(sessionToken);

        if (sessionValid) {
            // Renew session activity on valid requests
            boolean renewed = sessionManagementService.renewSession(sessionToken);
            if (!renewed) {
                log.warn("Failed to renew session for token: {}", maskToken(sessionToken));
                sendUnauthorizedResponse(httpResponse, "Session renewal failed");
                return;
            }
        } else {
            // Session invalid - send 401 for API endpoints
            if (requestPath.startsWith("/api/")) {
                sendUnauthorizedResponse(httpResponse, "Session expired or invalid");
                return;
            }
            // For web pages, let them proceed (they'll handle redirect to login)
        }

        chain.doFilter(request, response);
    }

    private boolean shouldSkipFilter(String requestPath) {
        return requestPath.startsWith("/api/auth/") ||
               requestPath.startsWith("/static/") ||
               requestPath.startsWith("/css/") ||
               requestPath.startsWith("/js/") ||
               requestPath.startsWith("/images/") ||
               requestPath.endsWith(".html") ||
               requestPath.endsWith(".css") ||
               requestPath.endsWith(".js") ||
               requestPath.endsWith(".png") ||
               requestPath.endsWith(".jpg") ||
               requestPath.endsWith(".ico") ||
               requestPath.equals("/") ||
               requestPath.equals("/login.html") ||
               requestPath.equals("/signup.html");
    }

    private String extractSessionToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(String.format("{\"error\": \"%s\", \"code\": \"SESSION_EXPIRED\"}", message));
    }

    private String maskToken(String token) {
        if (token == null || token.length() < 8) {
            return "***";
        }
        return token.substring(0, 4) + "***" + token.substring(token.length() - 4);
    }
}