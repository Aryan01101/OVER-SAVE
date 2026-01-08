package com.example.budgettracker.service;

import com.example.budgettracker.dto.*;
import com.example.budgettracker.model.*;
import com.example.budgettracker.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final IdpAccountRepository idpAccountRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionManagementService sessionManagementService;
    private final AccountRepository accountRepository;
    private final CategoryService categoryService;

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int RATE_LIMIT_MINUTES = 15;
    private static final int SESSION_HOURS = 24;

    @Override
    @Transactional
    public AuthResponse login(LoginRequest loginRequest, String ipAddress) {
        try {
            // Check rate limiting
            if (isRateLimited(loginRequest.getEmail(), ipAddress)) {
                recordLoginAttempt(loginRequest.getEmail(), ipAddress, false);
                return AuthResponse.error("Too many login attempts. Please try again later.");
            }

            // Find user by email
            Optional<User> userOpt = userRepository.findByEmail(loginRequest.getEmail());
            if (userOpt.isEmpty()) {
                recordLoginAttempt(loginRequest.getEmail(), ipAddress, false);
                return AuthResponse.error("Invalid credentials");
            }

            User user = userOpt.get();

            // Verify password
            if (!passwordEncoder.matches(loginRequest.getPassword(), user.getHashedPassword())) {
                recordLoginAttempt(loginRequest.getEmail(), ipAddress, false);
                return AuthResponse.error("Invalid credentials");
            }

            // Create secure session using SessionManagementService
            Session session = sessionManagementService.createSession(user, ipAddress, "");
            recordLoginAttempt(loginRequest.getEmail(), ipAddress, true);

            log.info("User {} logged in successfully", user.getEmail());

            return AuthResponse.success(
                    session.getSessionToken(),
                    user.getUserId(),
                    user.getEmail(),
                    user.getFirstName(),
                    user.getLastName(),
                    "/html/oversave-dashboard.html"
            );

        } catch (Exception e) {
            log.error("Login error for email {}: {}", loginRequest.getEmail(), e.getMessage());
            recordLoginAttempt(loginRequest.getEmail(), ipAddress, false);
            return AuthResponse.error("Login failed. Please try again.");
        }
    }

    @Override
    @Transactional
    public AuthResponse signup(SignupRequest signupRequest, String ipAddress) {
        try {
            // Check if user already exists
            if (userRepository.findByEmail(signupRequest.getEmail()).isPresent()) {
                return AuthResponse.error("Email address is already registered");
            }

            // Create new user
            User user = User.builder()
                    .email(signupRequest.getEmail())
                    .hashedPassword(passwordEncoder.encode(signupRequest.getPassword()))
                    .firstName(signupRequest.getFirstName())
                    .middleName(signupRequest.getMiddleName())
                    .lastName(signupRequest.getLastName())
                    .allowNotificationEmail(signupRequest.getAllowNotificationEmail())
                    .budgetCoin(0L)
                    .build();

            user = userRepository.save(user);

            // Create default CASH account for new user
            Account cashAccount = Account.builder()
                    .user(user)
                    .name("Main Account")
                    .accountType(com.example.budgettracker.model.enums.AccountType.CASH)
                    .balance(java.math.BigDecimal.ZERO)
                    .build();
            accountRepository.save(cashAccount);
            log.info("Created default CASH account for user {}", user.getEmail());

            categoryService.ensureSystemCategoriesForUser(user.getUserId());

            // Create secure session using SessionManagementService
            Session session = sessionManagementService.createSession(user, ipAddress, "");

            log.info("New user {} registered successfully", user.getEmail());

            return AuthResponse.success(
                    session.getSessionToken(),
                    user.getUserId(),
                    user.getEmail(),
                    user.getFirstName(),
                    user.getLastName(),
                    "/html/oversave-dashboard.html"
            );

        } catch (Exception e) {
            log.error("Signup error for email {}: {}", signupRequest.getEmail(), e.getMessage());
            return AuthResponse.error("Registration failed. Please try again.");
        }
    }

    @Override
    @Transactional
    public AuthResponse loginWithIdp(IdpLoginRequest idpLoginRequest, String ipAddress) {
        try {
            // Check rate limiting (using provider+subjectId as identifier)
            String idpIdentifier = idpLoginRequest.getProvider() + ":" + idpLoginRequest.getSubjectId();
            if (isRateLimited(idpIdentifier, ipAddress)) {
                recordLoginAttempt(idpIdentifier, ipAddress, false);
                return AuthResponse.error("Too many login attempts. Please try again later.");
            }

            // Find existing IdP account
            Optional<IdpAccount> idpAccountOpt = idpAccountRepository.findByProviderAndSubjectId(
                    idpLoginRequest.getProvider(),
                    idpLoginRequest.getSubjectId()
            );

            User user;
            if (idpAccountOpt.isPresent()) {
                // Existing IdP account
                user = idpAccountOpt.get().getUser();
            } else {
                // New IdP account - this would require additional user info from IdP
                recordLoginAttempt(idpIdentifier, ipAddress, false);
                return AuthResponse.error("IdP account not linked. Please sign up first.");
            }

            // Create secure session using SessionManagementService
            Session session = sessionManagementService.createSession(user, ipAddress, "");
            recordLoginAttempt(idpIdentifier, ipAddress, true);

            log.info("User {} logged in via IdP {} successfully", user.getEmail(), idpLoginRequest.getProvider());

            return AuthResponse.success(
                    session.getSessionToken(),
                    user.getUserId(),
                    user.getEmail(),
                    user.getFirstName(),
                    user.getLastName(),
                    "/html/oversave-dashboard.html"
            );

        } catch (Exception e) {
            log.error("IdP login error for provider {}: {}", idpLoginRequest.getProvider(), e.getMessage());
            String idpIdentifier = idpLoginRequest.getProvider() + ":" + idpLoginRequest.getSubjectId();
            recordLoginAttempt(idpIdentifier, ipAddress, false);
            return AuthResponse.error("IdP login failed. Please try again.");
        }
    }

    @Override
    public boolean isRateLimited(String email, String ipAddress) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(RATE_LIMIT_MINUTES);

        // Check failed attempts by email
        List<LoginAttempt> emailAttempts = loginAttemptRepository.findFailedAttemptsSince(email, since);
        if (emailAttempts.size() >= MAX_LOGIN_ATTEMPTS) {
            return true;
        }

        // Check failed attempts by IP address
        List<LoginAttempt> ipAttempts = loginAttemptRepository.findFailedAttemptsByIpSince(ipAddress, since);
        return ipAttempts.size() >= MAX_LOGIN_ATTEMPTS;
    }

    @Override
    @Transactional
    public void recordLoginAttempt(String email, String ipAddress, boolean successful) {
        LoginAttempt attempt = LoginAttempt.builder()
                .email(email)
                .ipAddress(ipAddress)
                .attemptTime(LocalDateTime.now())
                .successful(successful)
                .build();

        loginAttemptRepository.save(attempt);
    }

    @Override
    @Transactional
    public void logout(String sessionToken) {
        sessionManagementService.invalidateSession(sessionToken);
    }

    @Override
    public User getCurrentUser(String sessionToken) {
        return sessionManagementService.getUserFromSession(sessionToken).orElse(null);
    }

    @Override
    public boolean isValidSession(String sessionToken) {
        return sessionManagementService.isSessionValid(sessionToken);
    }

    private String generateSessionToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
