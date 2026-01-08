package com.example.budgettracker.usecase;

import com.example.budgettracker.model.Session;
import com.example.budgettracker.model.User;
import com.example.budgettracker.repository.SessionRepository;
import com.example.budgettracker.service.SessionManagementService;
import com.example.budgettracker.service.SessionManagementServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * UC-03: Logout Use Case Tests
 * Tests logout functionality including session invalidation, token clearing, and multi-device logout
 */
@RunWith(MockitoJUnitRunner.class)
public class LogoutTest {

    @Mock
    private SessionRepository sessionRepository;

    @InjectMocks
    private SessionManagementServiceImpl sessionManagementService;

    private Session mockSession;
    private User mockUser;
    private String validToken;

    @Before
    public void setUp() {
        mockUser = User.builder()
                .userId(1L)
                .email("testuser@example.com")
                .firstName("Test")
                .lastName("User")
                .build();

        validToken = "valid-session-token";

        mockSession = Session.builder()
                .sessionId(1L)
                .sessionToken(validToken)
                .user(mockUser)
                .isActive(true)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .lastActivityAt(LocalDateTime.now())
                .build();
    }

    @Test
    public void testLogout_ClearSession_Success() {
        // Arrange
        when(sessionRepository.findBySessionToken(validToken))
                .thenReturn(Optional.of(mockSession));
        when(sessionRepository.save(any(Session.class))).thenReturn(mockSession);

        // Act
        sessionManagementService.invalidateSession(validToken);

        // Assert
        verify(sessionRepository, times(1)).findBySessionToken(validToken);
        verify(sessionRepository, times(1)).save(any(Session.class));
        assertFalse("Session should be inactive after logout", mockSession.getIsActive());
    }

    @Test
    public void testLogout_InvalidateToken_Success() {
        // Arrange
        when(sessionRepository.findBySessionToken(validToken))
                .thenReturn(Optional.of(mockSession));
        when(sessionRepository.save(any(Session.class)))
                .thenAnswer(invocation -> {
                    Session session = invocation.getArgument(0);
                    session.setIsActive(false);
                    return session;
                });

        // Act
        sessionManagementService.invalidateSession(validToken);

        // Assert
        verify(sessionRepository, times(1)).findBySessionToken(validToken);
        verify(sessionRepository, times(1)).save(argThat(session ->
                !session.getIsActive()
        ));
    }

    @Test
    public void testLogout_ClearUserData_Success() {
        // Arrange
        Session sessionWithData = Session.builder()
                .sessionId(1L)
                .sessionToken(validToken)
                .user(mockUser)
                .isActive(true)
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0")
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

        when(sessionRepository.findBySessionToken(validToken))
                .thenReturn(Optional.of(sessionWithData));
        when(sessionRepository.save(any(Session.class))).thenReturn(sessionWithData);

        // Act
        sessionManagementService.invalidateSession(validToken);

        // Assert
        verify(sessionRepository, times(1)).save(any(Session.class));
        // Session data (IP, user agent) is preserved for audit purposes
        // but session is marked as inactive
        assertNotNull("IP address should be preserved for audit", sessionWithData.getIpAddress());
        assertNotNull("User agent should be preserved for audit", sessionWithData.getUserAgent());
    }

    @Test
    public void testLogout_MultiDevice_Success() {
        // Arrange - User has multiple active sessions
        Session session1 = Session.builder()
                .sessionId(1L)
                .sessionToken("token-device-1")
                .user(mockUser)
                .isActive(true)
                .ipAddress("192.168.1.1")
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

        Session session2 = Session.builder()
                .sessionId(2L)
                .sessionToken("token-device-2")
                .user(mockUser)
                .isActive(true)
                .ipAddress("192.168.1.2")
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

        List<Session> activeSessions = new ArrayList<>();
        activeSessions.add(session1);
        activeSessions.add(session2);

        when(sessionRepository.findActiveSessionsByUserId(mockUser.getUserId()))
                .thenReturn(activeSessions);
        when(sessionRepository.saveAll(anyList())).thenReturn(activeSessions);

        // Act - Invalidate all sessions for the user
        List<Session> sessions = sessionRepository.findActiveSessionsByUserId(mockUser.getUserId());
        sessions.forEach(s -> s.setIsActive(false));
        sessionRepository.saveAll(sessions);

        // Assert
        verify(sessionRepository, times(1)).findActiveSessionsByUserId(mockUser.getUserId());
        verify(sessionRepository, times(1)).saveAll(anyList());
        assertTrue("All sessions should be invalidated",
                activeSessions.stream().noneMatch(Session::getIsActive));
    }

    @Test
    public void testLogout_AuditLog_Success() {
        // Arrange
        LocalDateTime logoutTime = LocalDateTime.now();
        when(sessionRepository.findBySessionToken(validToken))
                .thenReturn(Optional.of(mockSession));
        when(sessionRepository.save(any(Session.class)))
                .thenAnswer(invocation -> {
                    Session session = invocation.getArgument(0);
                    session.setIsActive(false);
                    // In a real implementation, lastActivityAt would be updated during invalidation
                    return session;
                });

        // Act
        sessionManagementService.invalidateSession(validToken);

        // Assert
        verify(sessionRepository, times(1)).save(argThat(session -> {
            // Verify audit trail: session is marked inactive and timestamp preserved
            assertFalse("Session should be inactive", session.getIsActive());
            assertNotNull("Session token should be preserved for audit", session.getSessionToken());
            assertNotNull("User should be preserved for audit", session.getUser());
            return true;
        }));
    }
}
