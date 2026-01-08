package com.example.budgettracker.service;

import com.example.budgettracker.model.Session;
import com.example.budgettracker.model.User;
import com.example.budgettracker.repository.SessionRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(org.mockito.junit.MockitoJUnitRunner.class)
public class SessionManagementServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final String USER_EMAIL = "test@example.com";
    private static final String IP_ADDRESS = "192.168.1.1";
    private static final String USER_AGENT = "Mozilla/5.0";
    private static final String SESSION_SECRET = "testSecretKey123";

    @Mock
    private SessionRepository sessionRepository;

    @Spy
    @InjectMocks
    private SessionManagementServiceImpl sessionService;

    private User testUser;

    @Before
    public void setUp() {
        testUser = new User();
        testUser.setUserId(USER_ID);
        testUser.setEmail(USER_EMAIL);

        // Set the session secret using reflection
        ReflectionTestUtils.setField(sessionService, "sessionSecret", SESSION_SECRET);
    }

    // ==================== createSession Tests ====================

    @Test
    public void createSession_succeeds_whenInputsValid() {
        when(sessionRepository.findActiveSessionsByUserId(USER_ID)).thenReturn(Collections.emptyList());
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> {
            Session session = invocation.getArgument(0);
            session.setSessionId(100L);
            return session;
        });

        Session result = sessionService.createSession(testUser, IP_ADDRESS, USER_AGENT);

        assertNotNull(result);
        assertThat(result.getSessionId(), is(100L));
        assertThat(result.getUser(), is(testUser));
        assertThat(result.getIpAddress(), is(IP_ADDRESS));
        assertThat(result.getUserAgent(), is(USER_AGENT));
        assertThat(result.getIsActive(), is(true));
        assertNotNull(result.getSessionToken());
        assertNotNull(result.getTokenSignature());
        assertNotNull(result.getIssuedAt());
        assertNotNull(result.getExpiresAt());
        assertNotNull(result.getLastActivityAt());

        verify(sessionRepository).save(any(Session.class));
    }

    @Test
    public void createSession_invalidatesExistingSessions_whenUserHasActiveSessions() {
        Session existingSession1 = Session.builder()
                .sessionId(10L)
                .sessionToken("token1")
                .isActive(true)
                .build();
        Session existingSession2 = Session.builder()
                .sessionId(20L)
                .sessionToken("token2")
                .isActive(true)
                .build();

        when(sessionRepository.findActiveSessionsByUserId(USER_ID))
                .thenReturn(Arrays.asList(existingSession1, existingSession2));
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> invocation.getArgument(0));

        sessionService.createSession(testUser, IP_ADDRESS, USER_AGENT);

        // Verify existing sessions were invalidated
        assertThat(existingSession1.getIsActive(), is(false));
        assertThat(existingSession2.getIsActive(), is(false));
        verify(sessionRepository, times(3)).save(any(Session.class)); // 2 invalidations + 1 new session
    }

    @Test(expected = RuntimeException.class)
    public void createSession_throws_whenRepositoryFails() {
        when(sessionRepository.findActiveSessionsByUserId(USER_ID)).thenReturn(Collections.emptyList());
        when(sessionRepository.save(any(Session.class))).thenThrow(new RuntimeException("Database error"));

        sessionService.createSession(testUser, IP_ADDRESS, USER_AGENT);
    }

    // ==================== validateSession Tests ====================

    @Test
    public void validateSession_returnsEmpty_whenTokenIsNull() {
        Optional<Session> result = sessionService.validateSession(null);

        assertFalse(result.isPresent());
        verify(sessionRepository, never()).findBySessionToken(any());
    }

    @Test
    public void validateSession_returnsEmpty_whenTokenIsEmpty() {
        Optional<Session> result = sessionService.validateSession("   ");

        assertFalse(result.isPresent());
        verify(sessionRepository, never()).findBySessionToken(any());
    }

    @Test
    public void validateSession_returnsEmpty_whenSessionNotFound() {
        when(sessionRepository.findBySessionToken("invalid-token")).thenReturn(Optional.empty());

        Optional<Session> result = sessionService.validateSession("invalid-token");

        assertFalse(result.isPresent());
    }

    @Test
    public void validateSession_returnsEmpty_whenSessionIsInactive() {
        String token = "valid-token";
        Session session = Session.builder()
                .sessionToken(token)
                .isActive(false)
                .build();

        when(sessionRepository.findBySessionToken(token)).thenReturn(Optional.of(session));

        Optional<Session> result = sessionService.validateSession(token);

        assertFalse(result.isPresent());
    }

    @Test
    public void validateSession_invalidatesAndReturnsEmpty_whenSessionExpired() {
        String token = "expired-token";
        LocalDateTime now = LocalDateTime.now();
        Session session = Session.builder()
                .sessionToken(token)
                .isActive(true)
                .expiresAt(now.minusHours(1)) // Expired 1 hour ago
                .lastActivityAt(now.minusMinutes(2))
                .tokenSignature("signature")
                .build();

        when(sessionRepository.findBySessionToken(token)).thenReturn(Optional.of(session));

        Optional<Session> result = sessionService.validateSession(token);

        assertFalse(result.isPresent());
        verify(sessionRepository).save(session);
        assertThat(session.getIsActive(), is(false));
    }

    @Test
    public void validateSession_invalidatesAndReturnsEmpty_whenSessionIdle() {
        String token = "idle-token";
        LocalDateTime now = LocalDateTime.now();
        Session session = Session.builder()
                .sessionToken(token)
                .isActive(true)
                .expiresAt(now.plusHours(1)) // Not expired
                .lastActivityAt(now.minusMinutes(10)) // Idle for 10 minutes (> 5 min timeout)
                .tokenSignature("signature")
                .build();

        when(sessionRepository.findBySessionToken(token)).thenReturn(Optional.of(session));

        Optional<Session> result = sessionService.validateSession(token);

        assertFalse(result.isPresent());
        verify(sessionRepository).save(session);
        assertThat(session.getIsActive(), is(false));
    }

    @Test
    public void validateSession_invalidatesAndReturnsEmpty_whenTokenTampered() {
        String token = "valid-token";
        String invalidSignature = "invalid-signature";
        LocalDateTime now = LocalDateTime.now();

        Session session = Session.builder()
                .sessionToken(token)
                .isActive(true)
                .expiresAt(now.plusHours(1))
                .lastActivityAt(now.minusMinutes(1))
                .tokenSignature(invalidSignature)
                .build();

        when(sessionRepository.findBySessionToken(token)).thenReturn(Optional.of(session));
        // The service will generate the correct signature and compare it to the stored one

        Optional<Session> result = sessionService.validateSession(token);

        assertFalse(result.isPresent());
        verify(sessionRepository).save(session);
        assertThat(session.getIsActive(), is(false));
    }

    @Test
    public void validateSession_returnsSession_whenSessionValid() {
        String token = "valid-token";
        LocalDateTime now = LocalDateTime.now();

        // First generate a valid signature
        String validSignature = sessionService.verifyTokenSignature(token, "temp")
                ? "temp" : generateValidSignature(token);

        Session session = Session.builder()
                .sessionToken(token)
                .isActive(true)
                .expiresAt(now.plusHours(1))
                .lastActivityAt(now.minusMinutes(1))
                .tokenSignature(validSignature)
                .build();

        when(sessionRepository.findBySessionToken(token)).thenReturn(Optional.of(session));
        // Mock the signature verification to return true
        doReturn(true).when(sessionService).verifyTokenSignature(eq(token), eq(validSignature));

        Optional<Session> result = sessionService.validateSession(token);

        assertTrue(result.isPresent());
        assertThat(result.get(), is(session));
        verify(sessionRepository, never()).save(any()); // Should not save if valid
    }

    // ==================== renewSession Tests ====================

    @Test
    public void renewSession_returnsTrue_whenSessionValid() {
        String token = "valid-token";
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime initialActivity = now.minusMinutes(2);

        Session session = Session.builder()
                .sessionToken(token)
                .user(testUser)
                .isActive(true)
                .expiresAt(now.plusHours(10))
                .lastActivityAt(initialActivity)
                .tokenSignature("signature")
                .build();

        doReturn(Optional.of(session)).when(sessionService).validateSession(token);
        when(sessionRepository.save(session)).thenReturn(session);

        boolean result = sessionService.renewSession(token);

        assertTrue(result);
        assertNotEquals(initialActivity, session.getLastActivityAt());
        verify(sessionRepository).save(session);
    }

    @Test
    public void renewSession_extendsExpiration_whenCloseToExpiry() {
        String token = "valid-token";
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime soonExpiry = now.plusMinutes(30); // Less than 1 hour

        Session session = Session.builder()
                .sessionToken(token)
                .user(testUser)
                .isActive(true)
                .expiresAt(soonExpiry)
                .lastActivityAt(now.minusMinutes(1))
                .tokenSignature("signature")
                .build();

        doReturn(Optional.of(session)).when(sessionService).validateSession(token);
        when(sessionRepository.save(session)).thenReturn(session);

        boolean result = sessionService.renewSession(token);

        assertTrue(result);
        assertTrue(session.getExpiresAt().isAfter(soonExpiry));
        assertTrue(session.getExpiresAt().isAfter(now.plusHours(23))); // Extended by 24 hours
        verify(sessionRepository).save(session);
    }

    @Test
    public void renewSession_returnsFalse_whenSessionInvalid() {
        String token = "invalid-token";

        doReturn(Optional.empty()).when(sessionService).validateSession(token);

        boolean result = sessionService.renewSession(token);

        assertFalse(result);
        verify(sessionRepository, never()).save(any());
    }

    // ==================== invalidateSession Tests ====================

    @Test
    public void invalidateSession_succeeds_whenSessionExists() {
        String token = "valid-token";
        Session session = Session.builder()
                .sessionToken(token)
                .user(testUser)
                .isActive(true)
                .build();

        when(sessionRepository.findBySessionToken(token)).thenReturn(Optional.of(session));
        when(sessionRepository.save(session)).thenReturn(session);

        sessionService.invalidateSession(token);

        assertThat(session.getIsActive(), is(false));
        verify(sessionRepository).save(session);
    }

    @Test
    public void invalidateSession_doesNothing_whenSessionNotFound() {
        String token = "invalid-token";

        when(sessionRepository.findBySessionToken(token)).thenReturn(Optional.empty());

        sessionService.invalidateSession(token);

        verify(sessionRepository, never()).save(any());
    }

    // ==================== cleanupExpiredSessions Tests ====================

    @Test
    public void cleanupExpiredSessions_invalidatesExpiredAndIdleSessions() {
        LocalDateTime now = LocalDateTime.now();
        Session expiredSession = Session.builder()
                .sessionId(1L)
                .isActive(true)
                .expiresAt(now.minusHours(1))
                .build();
        Session idleSession = Session.builder()
                .sessionId(2L)
                .isActive(true)
                .lastActivityAt(now.minusMinutes(10))
                .build();

        List<Session> expiredSessions = Arrays.asList(expiredSession, idleSession);
        when(sessionRepository.findExpiredSessions(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(expiredSessions);
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> invocation.getArgument(0));

        sessionService.cleanupExpiredSessions();

        assertThat(expiredSession.getIsActive(), is(false));
        assertThat(idleSession.getIsActive(), is(false));
        verify(sessionRepository, times(2)).save(any(Session.class));
    }

    @Test
    public void cleanupExpiredSessions_doesNothing_whenNoExpiredSessions() {
        when(sessionRepository.findExpiredSessions(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        sessionService.cleanupExpiredSessions();

        verify(sessionRepository, never()).save(any());
    }

    // ==================== isSessionValid Tests ====================

    @Test
    public void isSessionValid_returnsTrue_whenSessionValid() {
        String token = "valid-token";
        Session session = Session.builder().sessionToken(token).build();

        doReturn(Optional.of(session)).when(sessionService).validateSession(token);

        boolean result = sessionService.isSessionValid(token);

        assertTrue(result);
    }

    @Test
    public void isSessionValid_returnsFalse_whenSessionInvalid() {
        String token = "invalid-token";

        doReturn(Optional.empty()).when(sessionService).validateSession(token);

        boolean result = sessionService.isSessionValid(token);

        assertFalse(result);
    }

    // ==================== getUserFromSession Tests ====================

    @Test
    public void getUserFromSession_returnsUser_whenSessionValid() {
        String token = "valid-token";
        Session session = Session.builder()
                .sessionToken(token)
                .user(testUser)
                .build();

        doReturn(Optional.of(session)).when(sessionService).validateSession(token);

        Optional<User> result = sessionService.getUserFromSession(token);

        assertTrue(result.isPresent());
        assertThat(result.get(), is(testUser));
    }

    @Test
    public void getUserFromSession_returnsEmpty_whenSessionInvalid() {
        String token = "invalid-token";

        doReturn(Optional.empty()).when(sessionService).validateSession(token);

        Optional<User> result = sessionService.getUserFromSession(token);

        assertFalse(result.isPresent());
    }

    // ==================== generateSecureToken Tests ====================

    @Test
    public void generateSecureToken_generatesUniqueTokens() {
        LocalDateTime now = LocalDateTime.now();

        String token1 = sessionService.generateSecureToken(testUser, now);
        String token2 = sessionService.generateSecureToken(testUser, now.plusSeconds(1));

        assertNotNull(token1);
        assertNotNull(token2);
        assertNotEquals(token1, token2);
    }

    @Test
    public void generateSecureToken_includesUserAndTimestamp() {
        LocalDateTime now = LocalDateTime.now();

        String token = sessionService.generateSecureToken(testUser, now);

        assertNotNull(token);
        assertTrue(token.length() > 0);
        // Token should be base64 encoded
        assertFalse(token.contains(" "));
    }

    // ==================== verifyTokenSignature Tests ====================

    @Test
    public void verifyTokenSignature_returnsTrue_whenSignatureValid() {
        String token = "test-token";

        // Generate a valid signature
        ArgumentCaptor<Session> sessionCaptor = ArgumentCaptor.forClass(Session.class);
        when(sessionRepository.save(sessionCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionRepository.findActiveSessionsByUserId(USER_ID)).thenReturn(Collections.emptyList());

        Session createdSession = sessionService.createSession(testUser, IP_ADDRESS, USER_AGENT);
        String validToken = createdSession.getSessionToken();
        String validSignature = createdSession.getTokenSignature();

        boolean result = sessionService.verifyTokenSignature(validToken, validSignature);

        assertTrue(result);
    }

    @Test
    public void verifyTokenSignature_returnsFalse_whenSignatureInvalid() {
        String token = "test-token";
        String invalidSignature = "invalid-signature";

        boolean result = sessionService.verifyTokenSignature(token, invalidSignature);

        assertFalse(result);
    }

    @Test
    public void verifyTokenSignature_returnsFalse_whenTokenModified() {
        // Generate signature for one token
        ArgumentCaptor<Session> sessionCaptor = ArgumentCaptor.forClass(Session.class);
        when(sessionRepository.save(sessionCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionRepository.findActiveSessionsByUserId(USER_ID)).thenReturn(Collections.emptyList());

        Session createdSession = sessionService.createSession(testUser, IP_ADDRESS, USER_AGENT);
        String signature = createdSession.getTokenSignature();

        // Try to verify with different token
        String differentToken = "different-token";
        boolean result = sessionService.verifyTokenSignature(differentToken, signature);

        assertFalse(result);
    }

    // ==================== Helper Methods ====================

    private String generateValidSignature(String token) {
        // This is a helper to create a valid signature for testing
        // In real implementation, this would use the same HMAC logic as the service
        return "mocked-valid-signature";
    }
}
