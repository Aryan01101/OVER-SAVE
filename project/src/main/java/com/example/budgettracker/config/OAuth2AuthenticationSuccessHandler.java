package com.example.budgettracker.config;

import com.example.budgettracker.model.Account;
import com.example.budgettracker.model.Session;
import com.example.budgettracker.model.User;
import com.example.budgettracker.model.enums.AccountType;
import com.example.budgettracker.repository.AccountRepository;
import com.example.budgettracker.repository.UserRepository;
import com.example.budgettracker.service.SessionManagementService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

/**
 * Custom success handler for OAuth2 authentication
 * Handles user creation/login and session management after successful Google OAuth
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final SessionManagementService sessionManagementService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.frontend.url:http://localhost:8080}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                       HttpServletResponse response,
                                       Authentication authentication) throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        try {
            // Extract user info from Google
            String email = oAuth2User.getAttribute("email");
            String googleId = oAuth2User.getAttribute("sub"); // Google's unique user ID
            String firstName = oAuth2User.getAttribute("given_name");
            String lastName = oAuth2User.getAttribute("family_name");
            String profilePictureUrl = oAuth2User.getAttribute("picture");

            log.info("OAuth2 login attempt for email: {}", email);

            // Find or create user
            User user = findOrCreateUser(email, googleId, firstName, lastName, profilePictureUrl);

            // Create session
            String ipAddress = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            Session session = sessionManagementService.createSession(user, ipAddress, userAgent);

            log.info("User {} logged in successfully via Google OAuth", user.getEmail());

            // Redirect to dashboard with session token
            String redirectUrl = String.format("%s/html/oversave-dashboard.html?sessionToken=%s&userId=%d&email=%s&firstName=%s&lastName=%s",
                frontendUrl,
                session.getSessionToken(),
                user.getUserId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName()
            );

            getRedirectStrategy().sendRedirect(request, response, redirectUrl);

        } catch (Exception e) {
            log.error("Error during OAuth2 authentication", e);

            // Redirect to login with error
            String errorUrl = frontendUrl + "/login.html?error=oauth_failed";
            getRedirectStrategy().sendRedirect(request, response, errorUrl);
        }
    }

    private User findOrCreateUser(String email, String googleId, String firstName,
                                 String lastName, String profilePictureUrl) {
        // Try to find existing user by email
        Optional<User> existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            User user = existingUser.get();

            // Update Google ID and profile picture if not set
            boolean needsUpdate = false;

            if (user.getGoogleId() == null || !user.getGoogleId().equals(googleId)) {
                user.setGoogleId(googleId);
                needsUpdate = true;
            }

            if (profilePictureUrl != null && !profilePictureUrl.equals(user.getProfilePictureUrl())) {
                user.setProfilePictureUrl(profilePictureUrl);
                needsUpdate = true;
            }

            if (needsUpdate) {
                user = userRepository.save(user);
                log.info("Updated existing user {} with Google OAuth info", email);
            }

            return user;
        }

        // Create new user
        User newUser = User.builder()
            .email(email)
            .googleId(googleId)
            .hashedPassword(generateRandomPassword()) // Random password for OAuth users
            .firstName(firstName != null ? firstName : "User")
            .lastName(lastName != null ? lastName : "")
            .profilePictureUrl(profilePictureUrl)
            .allowNotificationEmail(false)
            .budgetCoin(0L)
            .build();

        newUser = userRepository.save(newUser);
        log.info("Created new user via Google OAuth: {}", email);

        // Create default CASH account
        Account cashAccount = Account.builder()
            .user(newUser)
            .name("Cash Account")
            .accountType(AccountType.CASH)
            .build();
        accountRepository.save(cashAccount);
        log.info("Created default CASH account for new OAuth user: {}", email);

        return newUser;
    }

    private String generateRandomPassword() {
        // Generate secure random password for OAuth users
        // They won't use it, but database requires a value
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        String randomPassword = Base64.getEncoder().encodeToString(randomBytes);
        return passwordEncoder.encode(randomPassword);
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
}
