package com.example.budgettracker.service;

import com.example.budgettracker.dto.PasswordResetResponse;
import com.example.budgettracker.model.PasswordResetToken;
import com.example.budgettracker.model.User;
import com.example.budgettracker.repository.PasswordResetTokenRepository;
import com.example.budgettracker.repository.UserRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(org.mockito.junit.MockitoJUnitRunner.class)
public class PasswordResetServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final String USER_EMAIL = "test@example.com";
    private static final String USER_FIRST_NAME = "John";
    private static final String VALID_TOKEN = "valid-token-12345";
    private static final String INVALID_TOKEN = "invalid-token";
    private static final String NEW_PASSWORD = "ValidPass123!";
    private static final String WEAK_PASSWORD = "weak";
    private static final String HASHED_PASSWORD = "$2a$10$hashedPassword";

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private SessionManagementService sessionManagementService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordResetServiceImpl passwordResetService;

    private User testUser;
    private PasswordResetToken testToken;

    @Before
    public void setUp() {
        testUser = new User();
        testUser.setUserId(USER_ID);
        testUser.setEmail(USER_EMAIL);
        testUser.setFirstName(USER_FIRST_NAME);
        testUser.setHashedPassword(HASHED_PASSWORD);

        testToken = PasswordResetToken.builder()
                .user(testUser)
                .token(VALID_TOKEN)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();
    }

    // ==================== requestReset Tests ====================

    @Test
    public void requestReset_succeeds_whenUserExists() {
        when(userRepository.findByEmail(USER_EMAIL.toLowerCase().trim())).thenReturn(Optional.of(testUser));
        when(tokenRepository.countValidTokensForUser(eq(testUser), any(LocalDateTime.class))).thenReturn(0);
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(emailService).sendPasswordResetEmail(any(), any(), any());

        PasswordResetResponse response = passwordResetService.requestReset(USER_EMAIL);

        assertTrue(response.isSuccess());
        assertThat(response.getMessage(), containsString("If your email is registered"));
        verify(tokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendPasswordResetEmail(eq(USER_EMAIL), eq(USER_FIRST_NAME), any());
    }

    @Test
    public void requestReset_succeeds_whenUserDoesNotExist() {
        when(userRepository.findByEmail(USER_EMAIL.toLowerCase().trim())).thenReturn(Optional.empty());

        PasswordResetResponse response = passwordResetService.requestReset(USER_EMAIL);

        assertTrue(response.isSuccess());
        assertThat(response.getMessage(), containsString("If your email is registered"));
        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(any(), any(), any());
    }

    @Test
    public void requestReset_succeedsButDoesNotSend_whenTooManyTokens() {
        when(userRepository.findByEmail(USER_EMAIL.toLowerCase().trim())).thenReturn(Optional.of(testUser));
        when(tokenRepository.countValidTokensForUser(eq(testUser), any(LocalDateTime.class))).thenReturn(3);

        PasswordResetResponse response = passwordResetService.requestReset(USER_EMAIL);

        assertTrue(response.isSuccess());
        assertThat(response.getMessage(), containsString("If your email is registered"));
        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(any(), any(), any());
    }

    @Test
    public void requestReset_createsTokenWithCorrectExpiry() {
        when(userRepository.findByEmail(USER_EMAIL.toLowerCase().trim())).thenReturn(Optional.of(testUser));
        when(tokenRepository.countValidTokensForUser(eq(testUser), any(LocalDateTime.class))).thenReturn(0);
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(emailService).sendPasswordResetEmail(any(), any(), any());

        passwordResetService.requestReset(USER_EMAIL);

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        PasswordResetToken savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getUser(), is(testUser));
        assertNotNull(savedToken.getToken());
        assertNotNull(savedToken.getExpiresAt());
        assertTrue(savedToken.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    @Test
    public void requestReset_handlesException_gracefully() {
        when(userRepository.findByEmail(USER_EMAIL.toLowerCase().trim()))
                .thenThrow(new RuntimeException("Database error"));

        PasswordResetResponse response = passwordResetService.requestReset(USER_EMAIL);

        assertFalse(response.isSuccess());
        assertThat(response.getMessage(), containsString("error occurred"));
    }

    // ==================== validateToken Tests ====================

    @Test
    public void validateToken_succeeds_whenTokenValid() {
        when(tokenRepository.findValidToken(eq(VALID_TOKEN.trim()), any(LocalDateTime.class)))
                .thenReturn(Optional.of(testToken));

        PasswordResetResponse response = passwordResetService.validateToken(VALID_TOKEN);

        assertTrue(response.isSuccess());
        assertThat(response.getMessage(), is("Token is valid"));
    }

    @Test
    public void validateToken_fails_whenTokenNull() {
        PasswordResetResponse response = passwordResetService.validateToken(null);

        assertFalse(response.isSuccess());
        assertThat(response.getMessage(), is("Invalid token"));
        verify(tokenRepository, never()).findValidToken(any(), any());
    }

    @Test
    public void validateToken_fails_whenTokenEmpty() {
        PasswordResetResponse response = passwordResetService.validateToken("   ");

        assertFalse(response.isSuccess());
        assertThat(response.getMessage(), is("Invalid token"));
        verify(tokenRepository, never()).findValidToken(any(), any());
    }

    @Test
    public void validateToken_fails_whenTokenNotFound() {
        when(tokenRepository.findValidToken(eq(INVALID_TOKEN.trim()), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        PasswordResetResponse response = passwordResetService.validateToken(INVALID_TOKEN);

        assertFalse(response.isSuccess());
        assertThat(response.getMessage(), containsString("Invalid or expired"));
    }

    @Test
    public void validateToken_handlesException_gracefully() {
        when(tokenRepository.findValidToken(eq(VALID_TOKEN.trim()), any(LocalDateTime.class)))
                .thenThrow(new RuntimeException("Database error"));

        PasswordResetResponse response = passwordResetService.validateToken(VALID_TOKEN);

        assertFalse(response.isSuccess());
        assertThat(response.getMessage(), containsString("error occurred"));
    }

    // ==================== resetPassword Tests ====================

    @Test
    public void resetPassword_succeeds_whenTokenValidAndPasswordStrong() {
        when(tokenRepository.findValidToken(eq(VALID_TOKEN.trim()), any(LocalDateTime.class)))
                .thenReturn(Optional.of(testToken));
        when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(HASHED_PASSWORD);
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(tokenRepository.save(testToken)).thenReturn(testToken);
        when(tokenRepository.invalidateAllTokensForUser(testUser)).thenReturn(1);
        doNothing().when(sessionManagementService).invalidateAllSessionsForUser(testUser);
        doNothing().when(emailService).sendPasswordChangedEmail(USER_EMAIL, USER_FIRST_NAME);

        PasswordResetResponse response = passwordResetService.resetPassword(VALID_TOKEN, NEW_PASSWORD);

        assertTrue(response.isSuccess());
        assertThat(response.getMessage(), containsString("successfully reset"));
        verify(userRepository).save(testUser);
        verify(tokenRepository).invalidateAllTokensForUser(testUser);
        verify(sessionManagementService).invalidateAllSessionsForUser(testUser);
        verify(emailService).sendPasswordChangedEmail(USER_EMAIL, USER_FIRST_NAME);
    }

    @Test
    public void resetPassword_fails_whenTokenNull() {
        PasswordResetResponse response = passwordResetService.resetPassword(null, NEW_PASSWORD);

        assertFalse(response.isSuccess());
        assertThat(response.getMessage(), is("Invalid token"));
        verify(tokenRepository, never()).findValidToken(any(), any());
    }

    @Test
    public void resetPassword_fails_whenPasswordWeak() {
        PasswordResetResponse response = passwordResetService.resetPassword(VALID_TOKEN, WEAK_PASSWORD);

        assertFalse(response.isSuccess());
        assertThat(response.getMessage(), containsString("security requirements"));
        verify(tokenRepository, never()).findValidToken(any(), any());
    }

    @Test
    public void resetPassword_fails_whenTokenInvalid() {
        String validPassword = "ValidPass123!";
        when(tokenRepository.findValidToken(eq(INVALID_TOKEN.trim()), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        PasswordResetResponse response = passwordResetService.resetPassword(INVALID_TOKEN, validPassword);

        assertFalse(response.isSuccess());
        assertThat(response.getMessage(), containsString("Invalid or expired"));
        verify(userRepository, never()).save(any());
    }

    @Test
    public void resetPassword_marksTokenAsUsed() {
        when(tokenRepository.findValidToken(eq(VALID_TOKEN.trim()), any(LocalDateTime.class)))
                .thenReturn(Optional.of(testToken));
        when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(HASHED_PASSWORD);
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(tokenRepository.save(testToken)).thenReturn(testToken);
        when(tokenRepository.invalidateAllTokensForUser(testUser)).thenReturn(1);
        doNothing().when(sessionManagementService).invalidateAllSessionsForUser(testUser);
        doNothing().when(emailService).sendPasswordChangedEmail(USER_EMAIL, USER_FIRST_NAME);

        passwordResetService.resetPassword(VALID_TOKEN, NEW_PASSWORD);

        verify(tokenRepository).save(testToken);
        // Token should be marked as used
    }

    @Test
    public void resetPassword_invalidatesAllUserSessions() {
        when(tokenRepository.findValidToken(eq(VALID_TOKEN.trim()), any(LocalDateTime.class)))
                .thenReturn(Optional.of(testToken));
        when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(HASHED_PASSWORD);
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(tokenRepository.save(testToken)).thenReturn(testToken);
        when(tokenRepository.invalidateAllTokensForUser(testUser)).thenReturn(1);
        doNothing().when(sessionManagementService).invalidateAllSessionsForUser(testUser);
        doNothing().when(emailService).sendPasswordChangedEmail(USER_EMAIL, USER_FIRST_NAME);

        passwordResetService.resetPassword(VALID_TOKEN, NEW_PASSWORD);

        verify(sessionManagementService).invalidateAllSessionsForUser(testUser);
    }

    // ==================== validatePasswordStrength Tests ====================

    @Test
    public void validatePasswordStrength_succeeds_whenPasswordMeetsAllRequirements() {
        PasswordResetResponse response = passwordResetService.validatePasswordStrength("ValidPass123!");

        assertTrue(response.isSuccess());
        assertThat(response.getMessage(), is("Password meets security requirements"));
    }

    @Test
    public void validatePasswordStrength_fails_whenPasswordTooShort() {
        PasswordResetResponse response = passwordResetService.validatePasswordStrength("Short1!");

        assertFalse(response.isSuccess());
        assertTrue(response.getErrors().stream()
                .anyMatch(error -> error.contains("12 characters")));
    }

    @Test
    public void validatePasswordStrength_fails_whenPasswordMissingUppercase() {
        PasswordResetResponse response = passwordResetService.validatePasswordStrength("lowercase123!");

        assertFalse(response.isSuccess());
        assertTrue(response.getErrors().stream()
                .anyMatch(error -> error.contains("uppercase")));
    }

    @Test
    public void validatePasswordStrength_fails_whenPasswordMissingNumber() {
        PasswordResetResponse response = passwordResetService.validatePasswordStrength("NoNumbersHere!");

        assertFalse(response.isSuccess());
        assertTrue(response.getErrors().stream()
                .anyMatch(error -> error.contains("number")));
    }

    @Test
    public void validatePasswordStrength_fails_whenPasswordMissingSpecialChar() {
        PasswordResetResponse response = passwordResetService.validatePasswordStrength("NoSpecialChar123");

        assertFalse(response.isSuccess());
        assertTrue(response.getErrors().stream()
                .anyMatch(error -> error.contains("special character")));
    }

    @Test
    public void validatePasswordStrength_fails_whenPasswordContainsCommonPatterns() {
        PasswordResetResponse response1 = passwordResetService.validatePasswordStrength("Password123!");
        PasswordResetResponse response2 = passwordResetService.validatePasswordStrength("MyPassword123456!");
        PasswordResetResponse response3 = passwordResetService.validatePasswordStrength("Qwerty123456!");

        assertFalse(response1.isSuccess());
        assertFalse(response2.isSuccess());
        assertFalse(response3.isSuccess());
        assertTrue(response1.getErrors().stream()
                .anyMatch(error -> error.contains("common patterns")));
    }

    @Test
    public void validatePasswordStrength_fails_whenPasswordNull() {
        PasswordResetResponse response = passwordResetService.validatePasswordStrength(null);

        assertFalse(response.isSuccess());
        assertTrue(response.getErrors().stream()
                .anyMatch(error -> error.contains("12 characters")));
    }

    // ==================== cleanupExpiredTokens Tests ====================

    @Test
    public void cleanupExpiredTokens_returnsCount_whenTokensDeleted() {
        when(tokenRepository.deleteExpiredTokens(any(LocalDateTime.class))).thenReturn(5);

        int result = passwordResetService.cleanupExpiredTokens();

        assertThat(result, is(5));
        verify(tokenRepository).deleteExpiredTokens(any(LocalDateTime.class));
    }

    @Test
    public void cleanupExpiredTokens_returnsZero_whenNoTokensExpired() {
        when(tokenRepository.deleteExpiredTokens(any(LocalDateTime.class))).thenReturn(0);

        int result = passwordResetService.cleanupExpiredTokens();

        assertThat(result, is(0));
        verify(tokenRepository).deleteExpiredTokens(any(LocalDateTime.class));
    }

    @Test
    public void cleanupExpiredTokens_returnsZero_whenExceptionOccurs() {
        when(tokenRepository.deleteExpiredTokens(any(LocalDateTime.class)))
                .thenThrow(new RuntimeException("Database error"));

        int result = passwordResetService.cleanupExpiredTokens();

        assertThat(result, is(0));
    }

    // ==================== Edge Cases ====================

    @Test
    public void requestReset_handlesEmailWithWhitespace() {
        String emailWithWhitespace = "  test@example.com  ";
        when(userRepository.findByEmail(USER_EMAIL.toLowerCase().trim())).thenReturn(Optional.of(testUser));
        when(tokenRepository.countValidTokensForUser(eq(testUser), any(LocalDateTime.class))).thenReturn(0);
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(emailService).sendPasswordResetEmail(any(), any(), any());

        PasswordResetResponse response = passwordResetService.requestReset(emailWithWhitespace);

        assertTrue(response.isSuccess());
        verify(userRepository).findByEmail(USER_EMAIL.toLowerCase().trim());
    }

    @Test
    public void validateToken_handlesTokenWithWhitespace() {
        String tokenWithWhitespace = "  " + VALID_TOKEN + "  ";
        when(tokenRepository.findValidToken(eq(VALID_TOKEN.trim()), any(LocalDateTime.class)))
                .thenReturn(Optional.of(testToken));

        PasswordResetResponse response = passwordResetService.validateToken(tokenWithWhitespace);

        assertTrue(response.isSuccess());
        verify(tokenRepository).findValidToken(eq(VALID_TOKEN.trim()), any(LocalDateTime.class));
    }

    @Test
    public void resetPassword_handlesTokenWithWhitespace() {
        String tokenWithWhitespace = "  " + VALID_TOKEN + "  ";
        when(tokenRepository.findValidToken(eq(VALID_TOKEN.trim()), any(LocalDateTime.class)))
                .thenReturn(Optional.of(testToken));
        when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(HASHED_PASSWORD);
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(tokenRepository.save(testToken)).thenReturn(testToken);
        when(tokenRepository.invalidateAllTokensForUser(testUser)).thenReturn(1);
        doNothing().when(sessionManagementService).invalidateAllSessionsForUser(testUser);
        doNothing().when(emailService).sendPasswordChangedEmail(USER_EMAIL, USER_FIRST_NAME);

        PasswordResetResponse response = passwordResetService.resetPassword(tokenWithWhitespace, NEW_PASSWORD);

        assertTrue(response.isSuccess());
        verify(tokenRepository).findValidToken(eq(VALID_TOKEN.trim()), any(LocalDateTime.class));
    }
}
