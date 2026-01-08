package com.example.budgettracker.service;

public interface EmailService {

    /**
     * Send password reset email with reset link
     * @param email User's email address
     * @param firstName User's first name
     * @param token Reset token
     */
    void sendPasswordResetEmail(String email, String firstName, String token);

    /**
     * Send password changed confirmation email
     * @param email User's email address
     * @param firstName User's first name
     */
    void sendPasswordChangedEmail(String email, String firstName);

    /**
     * Send OAuth account recovery email
     * @param email User's email address
     * @param firstName User's first name
     * @param provider OAuth provider name
     */
    void sendOAuthRecoveryEmail(String email, String firstName, String provider);

    void sendTestEmail(String email, String firstName);
    void sendMonthlySummaryEmail(String email, String firstName, String summaryHtml);

}