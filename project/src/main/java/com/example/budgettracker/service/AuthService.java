package com.example.budgettracker.service;

import com.example.budgettracker.dto.*;
import com.example.budgettracker.model.User;

public interface AuthService {

    AuthResponse login(LoginRequest loginRequest, String ipAddress);

    AuthResponse signup(SignupRequest signupRequest, String ipAddress);

    AuthResponse loginWithIdp(IdpLoginRequest idpLoginRequest, String ipAddress);

    boolean isRateLimited(String email, String ipAddress);

    void recordLoginAttempt(String email, String ipAddress, boolean successful);

    void logout(String sessionToken);

    User getCurrentUser(String sessionToken);

    boolean isValidSession(String sessionToken);
}