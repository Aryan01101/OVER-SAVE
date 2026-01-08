package com.example.budgettracker.usecase;

import com.example.budgettracker.dto.PasswordResetResponse;
import com.example.budgettracker.model.PasswordResetToken;
import com.example.budgettracker.model.User;
import com.example.budgettracker.repository.PasswordResetTokenRepository;
import com.example.budgettracker.repository.UserRepository;
import com.example.budgettracker.service.EmailService;
import com.example.budgettracker.service.PasswordResetServiceImpl;
import com.example.budgettracker.service.SessionManagementService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * UC-04: Reset Password Use Case Tests
 * Tests password reset functionality including token generation, validation, and password update
 */
@RunWith(MockitoJUnitRunner.class)
public class ResetPasswordTest {

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

    private User mockUser;
    private PasswordResetToken mockToken;
    private String validTokenString;

    @Before
    public void setUp() {
        mockUser = User.builder()
                .userId(1L)
                .email("testuser@example.com")
                .hashedPassword("oldHashedPassword")
                .firstName("Test")
                .lastName("User")
                .build();

        validTokenString = "valid-reset-token-12345";

        mockToken = PasswordResetToken.builder()
                .id(1L)
                .token(validTokenString)
                .user(mockUser)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .used(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    public void testResetPassword_RequestToken_Success() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(tokenRepository.countValidTokensForUser(any(User.class), any(LocalDateTime.class)))
                .thenReturn(0);
        when(tokenRepository.save(any(PasswordResetToken.class))).thenReturn(mockToken);
        doNothing().when(emailService).sendPasswordResetEmail(anyString(), anyString(), anyString());

        // Act
        PasswordResetResponse response = passwordResetService.requestReset("testuser@example.com");

        // Assert
        assertNotNull("Response should not be null", response);
        assertTrue("Response should indicate success", response.isSuccess());
        assertTrue("Message should confirm email sent",
                response.getMessage().contains("email"));
        verify(userRepository, times(1)).findByEmail(anyString());
        verify(tokenRepository, times(1)).save(any(PasswordResetToken.class));
        verify(emailService, times(1)).sendPasswordResetEmail(anyString(), anyString(), anyString());
    }

    @Test
    public void testResetPassword_ValidateToken_Success() {
        // Arrange
        when(tokenRepository.findValidToken(eq(validTokenString), any(LocalDateTime.class)))
                .thenReturn(Optional.of(mockToken));

        // Act
        PasswordResetResponse response = passwordResetService.validateToken(validTokenString);

        // Assert
        assertNotNull("Response should not be null", response);
        assertTrue("Token should be valid", response.isSuccess());
        assertEquals("Message should confirm token validity", "Token is valid", response.getMessage());
        verify(tokenRepository, times(1)).findValidToken(eq(validTokenString), any(LocalDateTime.class));
    }

    @Test
    public void testResetPassword_SetNewPassword_Success() {
        // Arrange
        String newPassword = "NewSecureP@ss123";
        when(tokenRepository.findValidToken(eq(validTokenString), any(LocalDateTime.class)))
                .thenReturn(Optional.of(mockToken));
        when(passwordEncoder.encode(anyString())).thenReturn("newHashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(tokenRepository.save(any(PasswordResetToken.class))).thenReturn(mockToken);

        // Act
        PasswordResetResponse response = passwordResetService.resetPassword(validTokenString, newPassword);

        // Assert
        assertNotNull("Response should not be null", response);
        assertTrue("Password reset should succeed", response.isSuccess());
        assertTrue("Message should confirm success",
                response.getMessage().contains("successfully reset"));
        verify(passwordEncoder, times(1)).encode(newPassword);
        verify(userRepository, times(1)).save(any(User.class));
        verify(tokenRepository, times(1)).save(any(PasswordResetToken.class));
    }

    @Test
    public void testResetPassword_ExpiredToken_Failure() {
        // Arrange - Token expired 1 hour ago
        PasswordResetToken expiredToken = PasswordResetToken.builder()
                .id(2L)
                .token("expired-token")
                .user(mockUser)
                .expiresAt(LocalDateTime.now().minusHours(1))
                .used(false)
                .createdAt(LocalDateTime.now().minusHours(2))
                .build();

        when(tokenRepository.findValidToken(eq("expired-token"), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        // Act
        PasswordResetResponse response = passwordResetService.validateToken("expired-token");

        // Assert
        assertNotNull("Response should not be null", response);
        assertFalse("Token should be invalid", response.isSuccess());
        assertTrue("Message should indicate expiration",
                response.getMessage().contains("expired") || response.getMessage().contains("Invalid"));
        verify(tokenRepository, times(1)).findValidToken(eq("expired-token"), any(LocalDateTime.class));
    }

    @Test
    public void testResetPassword_InvalidToken_Failure() {
        // Arrange
        String invalidToken = "non-existent-token";
        when(tokenRepository.findValidToken(eq(invalidToken), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        // Act
        PasswordResetResponse response = passwordResetService.validateToken(invalidToken);

        // Assert
        assertNotNull("Response should not be null", response);
        assertFalse("Token should be invalid", response.isSuccess());
        assertTrue("Message should indicate invalid token",
                response.getMessage().contains("Invalid") || response.getMessage().contains("expired"));
        verify(tokenRepository, times(1)).findValidToken(eq(invalidToken), any(LocalDateTime.class));
        verify(userRepository, never()).save(any(User.class));
    }
}
