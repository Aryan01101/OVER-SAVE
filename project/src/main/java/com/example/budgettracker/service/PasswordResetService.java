package com.example.budgettracker.service;

import com.example.budgettracker.dto.PasswordResetRequest;
import com.example.budgettracker.dto.PasswordResetResponse;

public interface PasswordResetService {

    /**
     * Request password reset - sends email if user exists
     * @param email User's email address
     * @return Always returns success to prevent email enumeration
     */
    PasswordResetResponse requestReset(String email);

    /**
     * Validate reset token
     * @param token Reset token from URL
     * @return Validation result
     */
    PasswordResetResponse validateToken(String token);

    /**
     * Reset password using token
     * @param token Reset token
     * @param newPassword New password
     * @return Reset result
     */
    PasswordResetResponse resetPassword(String token, String newPassword);

    /**
     * Validate password strength
     * @param password Password to validate
     * @return Validation result with details
     */
    PasswordResetResponse validatePasswordStrength(String password);

    /**
     * Cleanup expired tokens
     * @return Number of tokens cleaned up
     */
    int cleanupExpiredTokens();
}