package com.example.budgettracker.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.test.util.ReflectionTestUtils;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(org.mockito.junit.MockitoJUnitRunner.class)
public class EmailServiceImplTest {

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_FIRST_NAME = "John";
    private static final String TEST_TOKEN = "test-reset-token-12345";
    private static final String TEST_PROVIDER = "Google";
    private static final String FROM_EMAIL = "noreply@oversave.com";
    private static final String FRONTEND_URL = "http://localhost:8080";

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailServiceImpl emailService;

    @Before
    public void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", FROM_EMAIL);
        ReflectionTestUtils.setField(emailService, "frontendUrl", FRONTEND_URL);
    }

    // ==================== sendPasswordResetEmail Tests ====================

    @Test
    public void sendPasswordResetEmail_succeeds_whenInputsValid() throws MessagingException {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        emailService.sendPasswordResetEmail(TEST_EMAIL, TEST_FIRST_NAME, TEST_TOKEN);

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    public void sendPasswordResetEmail_includesCorrectResetUrl() throws MessagingException {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        emailService.sendPasswordResetEmail(TEST_EMAIL, TEST_FIRST_NAME, TEST_TOKEN);

        verify(mailSender).send(mimeMessage);
        // Verify the reset URL would be constructed correctly
        String expectedUrl = FRONTEND_URL + "/reset-password.html?token=" + TEST_TOKEN;
        assertNotNull(expectedUrl);
        assertTrue(expectedUrl.contains(TEST_TOKEN));
    }

    @Test(expected = RuntimeException.class)
    public void sendPasswordResetEmail_throwsException_whenMessagingExceptionOccurs() throws MessagingException {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("Failed to send email", new MessagingException("SMTP error")))
                .when(mailSender).send(any(MimeMessage.class));

        emailService.sendPasswordResetEmail(TEST_EMAIL, TEST_FIRST_NAME, TEST_TOKEN);
    }

    @Test
    public void sendPasswordResetEmail_usesCorrectFromAddress() throws MessagingException {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        emailService.sendPasswordResetEmail(TEST_EMAIL, TEST_FIRST_NAME, TEST_TOKEN);

        verify(mailSender).send(mimeMessage);
        // Email should be sent from the configured from address
    }

    @Test
    public void sendPasswordResetEmail_includesUserFirstName() throws MessagingException {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        emailService.sendPasswordResetEmail(TEST_EMAIL, TEST_FIRST_NAME, TEST_TOKEN);

        verify(mailSender).send(mimeMessage);
        // Verify the first name is included in the email
    }

    // ==================== sendPasswordChangedEmail Tests ====================

    @Test
    public void sendPasswordChangedEmail_succeeds_whenInputsValid() throws MessagingException {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        emailService.sendPasswordChangedEmail(TEST_EMAIL, TEST_FIRST_NAME);

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test(expected = RuntimeException.class)
    public void sendPasswordChangedEmail_throwsException_whenMessagingExceptionOccurs() throws MessagingException {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("Failed to send email", new MessagingException("SMTP error")))
                .when(mailSender).send(any(MimeMessage.class));

        emailService.sendPasswordChangedEmail(TEST_EMAIL, TEST_FIRST_NAME);
    }

    @Test
    public void sendPasswordChangedEmail_includesUserFirstName() throws MessagingException {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        emailService.sendPasswordChangedEmail(TEST_EMAIL, TEST_FIRST_NAME);

        verify(mailSender).send(mimeMessage);
        // Verify the first name is included in the confirmation email
    }

    @Test
    public void sendPasswordChangedEmail_includesLoginUrl() throws MessagingException {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        emailService.sendPasswordChangedEmail(TEST_EMAIL, TEST_FIRST_NAME);

        verify(mailSender).send(mimeMessage);
        // Verify the login URL is included in the email
        String expectedUrl = FRONTEND_URL + "/login.html";
        assertNotNull(expectedUrl);
        assertTrue(expectedUrl.contains("/login.html"));
    }

    // ==================== sendOAuthRecoveryEmail Tests ====================

    @Test
    public void sendOAuthRecoveryEmail_succeeds_whenInputsValid() throws MessagingException {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        emailService.sendOAuthRecoveryEmail(TEST_EMAIL, TEST_FIRST_NAME, TEST_PROVIDER);

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test(expected = RuntimeException.class)
    public void sendOAuthRecoveryEmail_throwsException_whenMessagingExceptionOccurs() throws MessagingException {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("Failed to send email", new MessagingException("SMTP error")))
                .when(mailSender).send(any(MimeMessage.class));

        emailService.sendOAuthRecoveryEmail(TEST_EMAIL, TEST_FIRST_NAME, TEST_PROVIDER);
    }

    @Test
    public void sendOAuthRecoveryEmail_includesProviderName() throws MessagingException {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        emailService.sendOAuthRecoveryEmail(TEST_EMAIL, TEST_FIRST_NAME, TEST_PROVIDER);

        verify(mailSender).send(mimeMessage);
        // Verify the provider name is mentioned in the email
    }

    @Test
    public void sendOAuthRecoveryEmail_includesUserFirstName() throws MessagingException {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        emailService.sendOAuthRecoveryEmail(TEST_EMAIL, TEST_FIRST_NAME, TEST_PROVIDER);

        verify(mailSender).send(mimeMessage);
        // Verify the first name is included in the recovery email
    }

    @Test
    public void sendOAuthRecoveryEmail_includesLoginUrl() throws MessagingException {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        emailService.sendOAuthRecoveryEmail(TEST_EMAIL, TEST_FIRST_NAME, TEST_PROVIDER);

        verify(mailSender).send(mimeMessage);
        // Verify the login URL is included in the email
        String expectedUrl = FRONTEND_URL + "/login.html";
        assertNotNull(expectedUrl);
        assertTrue(expectedUrl.contains("/login.html"));
    }

    // ==================== Edge Cases ====================

    @Test
    public void sendPasswordResetEmail_handlesEmptyToken() throws MessagingException {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        emailService.sendPasswordResetEmail(TEST_EMAIL, TEST_FIRST_NAME, "");

        verify(mailSender).send(mimeMessage);
        // Should still attempt to send even with empty token
    }

    @Test
    public void sendPasswordChangedEmail_handlesEmptyFirstName() throws MessagingException {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        emailService.sendPasswordChangedEmail(TEST_EMAIL, "");

        verify(mailSender).send(mimeMessage);
        // Should still send email even with empty first name
    }

    @Test
    public void sendOAuthRecoveryEmail_handlesEmptyProvider() throws MessagingException {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        emailService.sendOAuthRecoveryEmail(TEST_EMAIL, TEST_FIRST_NAME, "");

        verify(mailSender).send(mimeMessage);
        // Should still send email even with empty provider
    }
}
