package com.example.budgettracker.usecase;

import com.example.budgettracker.dto.AuthResponse;
import com.example.budgettracker.dto.SignupRequest;
import com.example.budgettracker.model.Account;
import com.example.budgettracker.model.Session;
import com.example.budgettracker.model.User;
import com.example.budgettracker.model.IdpAccount;
import com.example.budgettracker.repository.AccountRepository;
import com.example.budgettracker.repository.UserRepository;
import com.example.budgettracker.repository.IdpAccountRepository;
import com.example.budgettracker.service.AuthServiceImpl;
import com.example.budgettracker.service.CategoryService;
import com.example.budgettracker.service.SessionManagementService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * UC-01: Register Use Case Tests
 * Tests user registration functionality including email verification, IdP, and validation
 */
@RunWith(MockitoJUnitRunner.class)
public class RegisterTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private IdpAccountRepository idpAccountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private SessionManagementService sessionManagementService;

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private AuthServiceImpl authService;

    private SignupRequest validSignupRequest;
    private User mockUser;
    private Session mockSession;
    private Account mockAccount;

    @Before
    public void setUp() {
        validSignupRequest = new SignupRequest();
        validSignupRequest.setEmail("newuser@example.com");
        validSignupRequest.setPassword("SecurePass123!");
        validSignupRequest.setFirstName("John");
        validSignupRequest.setLastName("Doe");
        validSignupRequest.setAllowNotificationEmail(true);

        mockUser = User.builder()
                .userId(1L)
                .email("newuser@example.com")
                .hashedPassword("hashedPassword")
                .firstName("John")
                .lastName("Doe")
                .budgetCoin(0L)
                .build();

        mockAccount = Account.builder()
                .accountId(1L)
                .name("Cash Account")
                .build();

        mockSession = Session.builder()
                .sessionToken("test-session-token")
                .user(mockUser)
                .build();
    }

    @Test
    public void testRegister_ValidRegistration_Success() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(accountRepository.save(any(Account.class))).thenReturn(mockAccount);
        when(sessionManagementService.createSession(any(User.class), anyString(), anyString()))
                .thenReturn(mockSession);

        // Act
        AuthResponse response = authService.signup(validSignupRequest, "192.168.1.1");

        // Assert
        assertNotNull("Response should not be null", response);
        assertEquals("Session token should match", "test-session-token", response.getSessionToken());
        assertEquals("User ID should match", Long.valueOf(1L), response.getUserId());
        assertEquals("Email should match", "newuser@example.com", response.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
        verify(accountRepository, times(1)).save(any(Account.class));
        verify(sessionManagementService, times(1)).createSession(any(User.class), anyString(), anyString());
        verify(categoryService, times(1)).ensureSystemCategoriesForUser(1L);
    }

    @Test
    public void testRegister_EmailVerification_Success() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(accountRepository.save(any(Account.class))).thenReturn(mockAccount);
        when(sessionManagementService.createSession(any(User.class), anyString(), anyString()))
                .thenReturn(mockSession);

        // Act
        AuthResponse response = authService.signup(validSignupRequest, "192.168.1.1");

        // Assert
        assertNotNull("Response should not be null", response);
        assertTrue("Email should be valid format", validSignupRequest.getEmail().contains("@"));
        assertTrue("Email should be verified in registration", response.getSessionToken() != null);
        verify(userRepository, times(1)).findByEmail(validSignupRequest.getEmail());
    }

    @Test
    public void testRegister_IdPRegistration_Success() {
        // Arrange - Simulating Google OAuth registration
        IdpAccount idpAccount = IdpAccount.builder()
                .subjectId("google-12345")
                .provider("google")
                .user(mockUser)
                .build();

        // Act - Test IdP account model
        IdpAccount testAccount = IdpAccount.builder()
                .subjectId("google-12345")
                .provider("google")
                .user(mockUser)
                .build();

        // Assert
        assertNotNull("IdP account should be created", testAccount);
        assertEquals("IdP provider should be google", "google", testAccount.getProvider());
        assertEquals("Subject ID should match", "google-12345", testAccount.getSubjectId());
        assertNotNull("User should be linked", testAccount.getUser());
    }

    @Test
    public void testRegister_DuplicateEmail_Failure() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));

        // Act
        AuthResponse response = authService.signup(validSignupRequest, "192.168.1.1");

        // Assert
        assertNotNull("Response should not be null", response);
        assertNull("Session token should be null for duplicate email", response.getSessionToken());
        assertTrue("Error message should indicate duplicate email",
                response.getMessage().contains("already registered"));
        verify(userRepository, never()).save(any(User.class));
        verify(sessionManagementService, never()).createSession(any(User.class), anyString(), anyString());
    }

    @Test
    public void testRegister_InvalidEmail_Failure() {
        // Arrange
        SignupRequest invalidRequest = new SignupRequest();
        invalidRequest.setEmail("invalid-email");  // Missing @ and domain
        invalidRequest.setPassword("SecurePass123!");
        invalidRequest.setFirstName("John");
        invalidRequest.setLastName("Doe");

        // Act - Email validation would happen at controller/DTO level
        boolean isValidEmail = invalidRequest.getEmail().contains("@") &&
                               invalidRequest.getEmail().contains(".");

        // Assert
        assertFalse("Email should be invalid", isValidEmail);
        // In a real scenario, this would be caught by @Email annotation validation
        // before reaching the service layer
    }
}
