package com.example.budgettracker.service;

import com.example.budgettracker.dto.*;
import com.example.budgettracker.model.*;
import com.example.budgettracker.repository.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(org.mockito.junit.MockitoJUnitRunner.class)
public class AuthServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final String USER_EMAIL = "test@example.com";
    private static final String USER_PASSWORD = "Password123!";
    private static final String HASHED_PASSWORD = "$2a$10$hashedPassword";
    private static final String FIRST_NAME = "John";
    private static final String LAST_NAME = "Doe";
    private static final String IP_ADDRESS = "192.168.1.1";
    private static final String SESSION_TOKEN = "test-session-token";
    private static final String IDP_PROVIDER = "google";
    private static final String IDP_SUBJECT_ID = "google-123456";

    @Mock
    private UserRepository userRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private IdpAccountRepository idpAccountRepository;

    @Mock
    private LoginAttemptRepository loginAttemptRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private SessionManagementService sessionManagementService;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private Session testSession;
    private LoginRequest loginRequest;
    private SignupRequest signupRequest;

    @Before
    public void setUp() {
        testUser = new User();
        testUser.setUserId(USER_ID);
        testUser.setEmail(USER_EMAIL);
        testUser.setHashedPassword(HASHED_PASSWORD);
        testUser.setFirstName(FIRST_NAME);
        testUser.setLastName(LAST_NAME);

        testSession = Session.builder()
                .sessionToken(SESSION_TOKEN)
                .user(testUser)
                .isActive(true)
                .build();

        loginRequest = new LoginRequest();
        loginRequest.setEmail(USER_EMAIL);
        loginRequest.setPassword(USER_PASSWORD);

        signupRequest = new SignupRequest();
        signupRequest.setEmail(USER_EMAIL);
        signupRequest.setPassword(USER_PASSWORD);
        signupRequest.setFirstName(FIRST_NAME);
        signupRequest.setLastName(LAST_NAME);
        signupRequest.setAllowNotificationEmail(true);
    }

    // ==================== login Tests ====================

    @Test
    public void login_succeeds_whenCredentialsValid() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(USER_PASSWORD, HASHED_PASSWORD)).thenReturn(true);
        when(sessionManagementService.createSession(testUser, IP_ADDRESS, "")).thenReturn(testSession);
        when(loginAttemptRepository.findFailedAttemptsSince(eq(USER_EMAIL), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(loginAttemptRepository.findFailedAttemptsByIpSince(eq(IP_ADDRESS), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        AuthResponse response = authService.login(loginRequest, IP_ADDRESS);

        assertNotNull(response.getSessionToken());
        assertThat(response.getSessionToken(), is(SESSION_TOKEN));
        assertThat(response.getUserId(), is(USER_ID));
        assertThat(response.getEmail(), is(USER_EMAIL));
        verify(loginAttemptRepository).save(any(LoginAttempt.class));
    }

    @Test
    public void login_fails_whenUserNotFound() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.empty());
        when(loginAttemptRepository.findFailedAttemptsSince(eq(USER_EMAIL), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(loginAttemptRepository.findFailedAttemptsByIpSince(eq(IP_ADDRESS), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        AuthResponse response = authService.login(loginRequest, IP_ADDRESS);

        assertNull(response.getSessionToken());
        assertThat(response.getMessage(), is("Invalid credentials"));
        verify(loginAttemptRepository).save(any(LoginAttempt.class));
        verify(sessionManagementService, never()).createSession(any(), any(), any());
    }

    @Test
    public void login_fails_whenPasswordInvalid() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(USER_PASSWORD, HASHED_PASSWORD)).thenReturn(false);
        when(loginAttemptRepository.findFailedAttemptsSince(eq(USER_EMAIL), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(loginAttemptRepository.findFailedAttemptsByIpSince(eq(IP_ADDRESS), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        AuthResponse response = authService.login(loginRequest, IP_ADDRESS);

        assertNull(response.getSessionToken());
        assertThat(response.getMessage(), is("Invalid credentials"));
        verify(loginAttemptRepository).save(any(LoginAttempt.class));
        verify(sessionManagementService, never()).createSession(any(), any(), any());
    }

    @Test
    public void login_fails_whenRateLimited() {
        List<LoginAttempt> failedAttempts = Arrays.asList(
                new LoginAttempt(), new LoginAttempt(), new LoginAttempt(),
                new LoginAttempt(), new LoginAttempt()
        );
        when(loginAttemptRepository.findFailedAttemptsSince(eq(USER_EMAIL), any(LocalDateTime.class)))
                .thenReturn(failedAttempts);

        AuthResponse response = authService.login(loginRequest, IP_ADDRESS);

        assertNull(response.getSessionToken());
        assertThat(response.getMessage(), is("Too many login attempts. Please try again later."));
        verify(userRepository, never()).findByEmail(any());
        verify(loginAttemptRepository).save(any(LoginAttempt.class));
    }

    @Test
    public void login_recordsLoginAttempt_whenSuccessful() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(USER_PASSWORD, HASHED_PASSWORD)).thenReturn(true);
        when(sessionManagementService.createSession(testUser, IP_ADDRESS, "")).thenReturn(testSession);
        when(loginAttemptRepository.findFailedAttemptsSince(eq(USER_EMAIL), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(loginAttemptRepository.findFailedAttemptsByIpSince(eq(IP_ADDRESS), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        authService.login(loginRequest, IP_ADDRESS);

        ArgumentCaptor<LoginAttempt> attemptCaptor = ArgumentCaptor.forClass(LoginAttempt.class);
        verify(loginAttemptRepository).save(attemptCaptor.capture());
        LoginAttempt savedAttempt = attemptCaptor.getValue();
        assertThat(savedAttempt.getEmail(), is(USER_EMAIL));
        assertThat(savedAttempt.getIpAddress(), is(IP_ADDRESS));
        assertTrue(savedAttempt.getSuccessful());
    }

    // ==================== signup Tests ====================

    @Test
    public void signup_succeeds_whenEmailNotRegistered() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(USER_PASSWORD)).thenReturn(HASHED_PASSWORD);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId(USER_ID);
            return user;
        });
        when(sessionManagementService.createSession(any(User.class), eq(IP_ADDRESS), eq("")))
                .thenReturn(testSession);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(categoryService).ensureSystemCategoriesForUser(anyLong());

        AuthResponse response = authService.signup(signupRequest, IP_ADDRESS);

        assertNotNull(response.getSessionToken());
        assertThat(response.getSessionToken(), is(SESSION_TOKEN));
        assertThat(response.getEmail(), is(USER_EMAIL));
        verify(userRepository).save(any(User.class));
        verify(accountRepository).save(any(Account.class));
        verify(categoryService).ensureSystemCategoriesForUser(USER_ID);
    }

    @Test
    public void signup_fails_whenEmailAlreadyRegistered() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(testUser));

        AuthResponse response = authService.signup(signupRequest, IP_ADDRESS);

        assertNull(response.getSessionToken());
        assertThat(response.getMessage(), is("Email address is already registered"));
        verify(userRepository, never()).save(any());
        verify(sessionManagementService, never()).createSession(any(), any(), any());
    }

    @Test
    public void signup_createsDefaultCashAccount_whenSuccessful() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(USER_PASSWORD)).thenReturn(HASHED_PASSWORD);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId(USER_ID);
            return user;
        });
        when(sessionManagementService.createSession(any(User.class), eq(IP_ADDRESS), eq("")))
                .thenReturn(testSession);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(categoryService).ensureSystemCategoriesForUser(anyLong());

        authService.signup(signupRequest, IP_ADDRESS);

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        Account savedAccount = accountCaptor.getValue();
        assertThat(savedAccount.getName(), is("Main Account"));
        assertThat(savedAccount.getAccountType(), is(com.example.budgettracker.model.enums.AccountType.CASH));
    }

    @Test
    public void signup_encodesPassword_whenCreatingUser() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(USER_PASSWORD)).thenReturn(HASHED_PASSWORD);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId(USER_ID);
            return user;
        });
        when(sessionManagementService.createSession(any(User.class), eq(IP_ADDRESS), eq("")))
                .thenReturn(testSession);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(categoryService).ensureSystemCategoriesForUser(anyLong());

        authService.signup(signupRequest, IP_ADDRESS);

        verify(passwordEncoder).encode(USER_PASSWORD);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getHashedPassword(), is(HASHED_PASSWORD));
    }

    // ==================== loginWithIdp Tests ====================

    @Test
    public void loginWithIdp_succeeds_whenIdpAccountExists() {
        IdpAccount idpAccount = new IdpAccount();
        idpAccount.setUser(testUser);
        idpAccount.setProvider(IDP_PROVIDER);
        idpAccount.setSubjectId(IDP_SUBJECT_ID);

        IdpLoginRequest idpRequest = new IdpLoginRequest();
        idpRequest.setProvider(IDP_PROVIDER);
        idpRequest.setSubjectId(IDP_SUBJECT_ID);

        when(idpAccountRepository.findByProviderAndSubjectId(IDP_PROVIDER, IDP_SUBJECT_ID))
                .thenReturn(Optional.of(idpAccount));
        when(sessionManagementService.createSession(testUser, IP_ADDRESS, "")).thenReturn(testSession);
        when(loginAttemptRepository.findFailedAttemptsSince(any(), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(loginAttemptRepository.findFailedAttemptsByIpSince(eq(IP_ADDRESS), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        AuthResponse response = authService.loginWithIdp(idpRequest, IP_ADDRESS);

        assertNotNull(response.getSessionToken());
        assertThat(response.getSessionToken(), is(SESSION_TOKEN));
        assertThat(response.getUserId(), is(USER_ID));
        verify(loginAttemptRepository).save(any(LoginAttempt.class));
    }

    @Test
    public void loginWithIdp_fails_whenIdpAccountNotFound() {
        IdpLoginRequest idpRequest = new IdpLoginRequest();
        idpRequest.setProvider(IDP_PROVIDER);
        idpRequest.setSubjectId(IDP_SUBJECT_ID);

        when(idpAccountRepository.findByProviderAndSubjectId(IDP_PROVIDER, IDP_SUBJECT_ID))
                .thenReturn(Optional.empty());
        when(loginAttemptRepository.findFailedAttemptsSince(any(), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(loginAttemptRepository.findFailedAttemptsByIpSince(eq(IP_ADDRESS), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        AuthResponse response = authService.loginWithIdp(idpRequest, IP_ADDRESS);

        assertNull(response.getSessionToken());
        assertThat(response.getMessage(), is("IdP account not linked. Please sign up first."));
        verify(sessionManagementService, never()).createSession(any(), any(), any());
    }

    // ==================== isRateLimited Tests ====================

    @Test
    public void isRateLimited_returnsTrue_whenEmailHasMaxFailedAttempts() {
        List<LoginAttempt> failedAttempts = Arrays.asList(
                new LoginAttempt(), new LoginAttempt(), new LoginAttempt(),
                new LoginAttempt(), new LoginAttempt()
        );
        when(loginAttemptRepository.findFailedAttemptsSince(eq(USER_EMAIL), any(LocalDateTime.class)))
                .thenReturn(failedAttempts);

        boolean result = authService.isRateLimited(USER_EMAIL, IP_ADDRESS);

        assertTrue(result);
    }

    @Test
    public void isRateLimited_returnsTrue_whenIpHasMaxFailedAttempts() {
        List<LoginAttempt> failedAttempts = Arrays.asList(
                new LoginAttempt(), new LoginAttempt(), new LoginAttempt(),
                new LoginAttempt(), new LoginAttempt()
        );
        when(loginAttemptRepository.findFailedAttemptsSince(eq(USER_EMAIL), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(loginAttemptRepository.findFailedAttemptsByIpSince(eq(IP_ADDRESS), any(LocalDateTime.class)))
                .thenReturn(failedAttempts);

        boolean result = authService.isRateLimited(USER_EMAIL, IP_ADDRESS);

        assertTrue(result);
    }

    @Test
    public void isRateLimited_returnsFalse_whenNoExcessiveAttempts() {
        when(loginAttemptRepository.findFailedAttemptsSince(eq(USER_EMAIL), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(loginAttemptRepository.findFailedAttemptsByIpSince(eq(IP_ADDRESS), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        boolean result = authService.isRateLimited(USER_EMAIL, IP_ADDRESS);

        assertFalse(result);
    }

    // ==================== logout Tests ====================

    @Test
    public void logout_invalidatesSession() {
        doNothing().when(sessionManagementService).invalidateSession(SESSION_TOKEN);

        authService.logout(SESSION_TOKEN);

        verify(sessionManagementService).invalidateSession(SESSION_TOKEN);
    }

    // ==================== getCurrentUser Tests ====================

    @Test
    public void getCurrentUser_returnsUser_whenSessionValid() {
        when(sessionManagementService.getUserFromSession(SESSION_TOKEN))
                .thenReturn(Optional.of(testUser));

        User result = authService.getCurrentUser(SESSION_TOKEN);

        assertNotNull(result);
        assertThat(result, is(testUser));
    }

    @Test
    public void getCurrentUser_returnsNull_whenSessionInvalid() {
        when(sessionManagementService.getUserFromSession(SESSION_TOKEN))
                .thenReturn(Optional.empty());

        User result = authService.getCurrentUser(SESSION_TOKEN);

        assertNull(result);
    }

    // ==================== isValidSession Tests ====================

    @Test
    public void isValidSession_returnsTrue_whenSessionValid() {
        when(sessionManagementService.isSessionValid(SESSION_TOKEN)).thenReturn(true);

        boolean result = authService.isValidSession(SESSION_TOKEN);

        assertTrue(result);
    }

    @Test
    public void isValidSession_returnsFalse_whenSessionInvalid() {
        when(sessionManagementService.isSessionValid(SESSION_TOKEN)).thenReturn(false);

        boolean result = authService.isValidSession(SESSION_TOKEN);

        assertFalse(result);
    }

    // ==================== recordLoginAttempt Tests ====================

    @Test
    public void recordLoginAttempt_savesSuccessfulAttempt() {
        when(loginAttemptRepository.save(any(LoginAttempt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        authService.recordLoginAttempt(USER_EMAIL, IP_ADDRESS, true);

        ArgumentCaptor<LoginAttempt> captor = ArgumentCaptor.forClass(LoginAttempt.class);
        verify(loginAttemptRepository).save(captor.capture());
        LoginAttempt saved = captor.getValue();
        assertThat(saved.getEmail(), is(USER_EMAIL));
        assertThat(saved.getIpAddress(), is(IP_ADDRESS));
        assertTrue(saved.getSuccessful());
    }

    @Test
    public void recordLoginAttempt_savesFailedAttempt() {
        when(loginAttemptRepository.save(any(LoginAttempt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        authService.recordLoginAttempt(USER_EMAIL, IP_ADDRESS, false);

        ArgumentCaptor<LoginAttempt> captor = ArgumentCaptor.forClass(LoginAttempt.class);
        verify(loginAttemptRepository).save(captor.capture());
        LoginAttempt saved = captor.getValue();
        assertThat(saved.getEmail(), is(USER_EMAIL));
        assertThat(saved.getIpAddress(), is(IP_ADDRESS));
        assertFalse(saved.getSuccessful());
    }
}
