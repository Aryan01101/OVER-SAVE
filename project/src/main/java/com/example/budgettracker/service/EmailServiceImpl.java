package com.example.budgettracker.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.email.from:noreply@oversave.com}")
    private String fromEmail;

    @Value("${app.frontend.url:http://localhost:8080}")
    private String frontendUrl;

    @Override
    public void sendPasswordResetEmail(String email, String firstName, String token) {
        try {
            String resetUrl = frontendUrl + "/reset-password.html?token=" + token;

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject("Reset Your OVER-SAVE Password");

            String htmlContent = buildPasswordResetEmail(firstName, resetUrl);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("Password reset email sent to: {}", email);

        } catch (MessagingException e) {
            log.error("Failed to send password reset email to: {}", email, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    @Override
    public void sendPasswordChangedEmail(String email, String firstName) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject("Your OVER-SAVE Password Has Been Changed");

            String htmlContent = buildPasswordChangedEmail(firstName);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("Password changed confirmation email sent to: {}", email);

        } catch (MessagingException e) {
            log.error("Failed to send password changed email to: {}", email, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    @Override
    public void sendOAuthRecoveryEmail(String email, String firstName, String provider) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject("OVER-SAVE Account Recovery Instructions");

            String htmlContent = buildOAuthRecoveryEmail(firstName, provider);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("OAuth recovery email sent to: {}", email);

        } catch (MessagingException e) {
            log.error("Failed to send OAuth recovery email to: {}", email, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    private String buildPasswordResetEmail(String firstName, String resetUrl) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Reset Your Password</title>
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;">
                <div style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); padding: 30px; text-align: center; border-radius: 10px 10px 0 0;">
                    <h1 style="color: white; margin: 0; font-size: 28px;">💰 OVER-SAVE</h1>
                    <p style="color: #f0f0f0; margin: 10px 0 0 0;">Password Reset Request</p>
                </div>

                <div style="background: #f8f9fa; padding: 30px; border-radius: 0 0 10px 10px; border: 1px solid #e9ecef;">
                    <h2 style="color: #495057; margin-top: 0;">Hi %s! 👋</h2>

                    <p>We received a request to reset the password for your OVER-SAVE account.</p>

                    <div style="background: #fff; border: 1px solid #dee2e6; border-radius: 8px; padding: 20px; margin: 20px 0;">
                        <p style="margin: 0 0 15px 0; font-weight: bold;">🔐 Click the button below to reset your password:</p>
                        <a href="%s" style="display: inline-block; background: #007bff; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; font-weight: bold;">Reset My Password</a>
                    </div>

                    <div style="background: #fff3cd; border: 1px solid #ffeaa7; border-radius: 5px; padding: 15px; margin: 20px 0;">
                        <p style="margin: 0; color: #856404;"><strong>⚠️ Important Security Information:</strong></p>
                        <ul style="margin: 10px 0 0 0; color: #856404;">
                            <li>This link expires in <strong>15 minutes</strong> for your security</li>
                            <li>You can only use this link once</li>
                            <li>If you didn't request this reset, please ignore this email</li>
                        </ul>
                    </div>

                    <p style="font-size: 14px; color: #6c757d; margin-top: 30px;">
                        If the button doesn't work, copy and paste this link into your browser:<br>
                        <a href="%s" style="color: #007bff; word-break: break-all;">%s</a>
                    </p>

                    <hr style="border: none; border-top: 1px solid #dee2e6; margin: 30px 0;">

                    <p style="font-size: 12px; color: #868e96; text-align: center;">
                        This email was sent by OVER-SAVE Budget Tracker<br>
                        If you didn't request this password reset, no action is needed.
                    </p>
                </div>
            </body>
            </html>
            """.formatted(firstName, resetUrl, resetUrl, resetUrl);
    }

    private String buildPasswordChangedEmail(String firstName) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Password Changed Successfully</title>
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;">
                <div style="background: linear-gradient(135deg, #28a745 0%%, #20c997 100%%); padding: 30px; text-align: center; border-radius: 10px 10px 0 0;">
                    <h1 style="color: white; margin: 0; font-size: 28px;">💰 OVER-SAVE</h1>
                    <p style="color: #f0f0f0; margin: 10px 0 0 0;">Password Changed Successfully</p>
                </div>

                <div style="background: #f8f9fa; padding: 30px; border-radius: 0 0 10px 10px; border: 1px solid #e9ecef;">
                    <h2 style="color: #495057; margin-top: 0;">Hi %s! 👋</h2>

                    <div style="background: #d4edda; border: 1px solid #c3e6cb; border-radius: 5px; padding: 15px; margin: 20px 0;">
                        <p style="margin: 0; color: #155724;"><strong>✅ Your password has been successfully changed!</strong></p>
                    </div>

                    <p>Your OVER-SAVE account password was changed successfully. You can now log in with your new password.</p>

                    <div style="background: #fff; border: 1px solid #dee2e6; border-radius: 8px; padding: 20px; margin: 20px 0;">
                        <h4 style="margin: 0 0 10px 0; color: #495057;">🔒 Security Details:</h4>
                        <ul style="margin: 0; padding-left: 20px;">
                            <li>All existing sessions have been logged out for security</li>
                            <li>You'll need to log in again with your new password</li>
                            <li>All password reset tokens have been invalidated</li>
                        </ul>
                    </div>

                    <div style="background: #fff3cd; border: 1px solid #ffeaa7; border-radius: 5px; padding: 15px; margin: 20px 0;">
                        <p style="margin: 0; color: #856404;"><strong>⚠️ Didn't change your password?</strong></p>
                        <p style="margin: 10px 0 0 0; color: #856404;">If you didn't make this change, please contact our support team immediately.</p>
                    </div>

                    <p style="text-align: center; margin: 30px 0;">
                        <a href="%s" style="display: inline-block; background: #007bff; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; font-weight: bold;">Login to OVER-SAVE</a>
                    </p>

                    <hr style="border: none; border-top: 1px solid #dee2e6; margin: 30px 0;">

                    <p style="font-size: 12px; color: #868e96; text-align: center;">
                        This email was sent by OVER-SAVE Budget Tracker<br>
                        This is an automated security notification.
                    </p>
                </div>
            </body>
            </html>
            """.formatted(firstName, frontendUrl + "/login.html");
    }

    private String buildOAuthRecoveryEmail(String firstName, String provider) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Account Recovery Instructions</title>
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;">
                <div style="background: linear-gradient(135deg, #ffc107 0%%, #ff8c00 100%%); padding: 30px; text-align: center; border-radius: 10px 10px 0 0;">
                    <h1 style="color: white; margin: 0; font-size: 28px;">💰 OVER-SAVE</h1>
                    <p style="color: #f0f0f0; margin: 10px 0 0 0;">Account Recovery</p>
                </div>

                <div style="background: #f8f9fa; padding: 30px; border-radius: 0 0 10px 10px; border: 1px solid #e9ecef;">
                    <h2 style="color: #495057; margin-top: 0;">Hi %s! 👋</h2>

                    <p>We received a password reset request for your OVER-SAVE account, but it looks like your account is linked to %s.</p>

                    <div style="background: #cce5ff; border: 1px solid #99d6ff; border-radius: 5px; padding: 15px; margin: 20px 0;">
                        <p style="margin: 0; color: #004085;"><strong>🔗 Your account is connected via %s</strong></p>
                        <p style="margin: 10px 0 0 0; color: #004085;">You don't need to reset a password - just log in using %s!</p>
                    </div>

                    <div style="background: #fff; border: 1px solid #dee2e6; border-radius: 8px; padding: 20px; margin: 20px 0;">
                        <h4 style="margin: 0 0 15px 0; color: #495057;">🔐 How to access your account:</h4>
                        <ol style="margin: 0; padding-left: 20px;">
                            <li>Go to the OVER-SAVE login page</li>
                            <li>Click "Continue with %s"</li>
                            <li>Log in with your %s credentials</li>
                            <li>You'll be automatically signed in to OVER-SAVE</li>
                        </ol>
                    </div>

                    <p style="text-align: center; margin: 30px 0;">
                        <a href="%s" style="display: inline-block; background: #007bff; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; font-weight: bold;">Login to OVER-SAVE</a>
                    </p>

                    <div style="background: #fff3cd; border: 1px solid #ffeaa7; border-radius: 5px; padding: 15px; margin: 20px 0;">
                        <p style="margin: 0; color: #856404;"><strong>💡 Need help?</strong></p>
                        <p style="margin: 10px 0 0 0; color: #856404;">If you're having trouble accessing your %s account, please contact %s support first.</p>
                    </div>

                    <hr style="border: none; border-top: 1px solid #dee2e6; margin: 30px 0;">

                    <p style="font-size: 12px; color: #868e96; text-align: center;">
                        This email was sent by OVER-SAVE Budget Tracker<br>
                        If you didn't request this, no action is needed.
                    </p>
                </div>
            </body>
            </html>
            """.formatted(firstName, provider, provider, provider, provider, provider, frontendUrl + "/login.html", provider, provider);
    }

    @Override
    public void sendTestEmail(String email, String firstName) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject("OVER-SAVE Test Email");

            String html = """
            <!DOCTYPE html>
            <html>
            <body style="font-family: Arial, sans-serif; line-height:1.6; color:#333; max-width:600px; margin:auto;">
                <div style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                            padding: 25px; text-align: center; border-radius: 10px 10px 0 0;">
                    <h1 style="color: white; margin: 0;">💰 OVER-SAVE</h1>
                    <p style="color: #e2e2e2; margin: 8px 0 0;">Test Email Successful</p>
                </div>
                <div style="background: #f9f9f9; padding: 30px; border: 1px solid #eee; border-radius: 0 0 10px 10px;">
                    <h2>Hi %s 👋</h2>
                    <p>This is a test email confirming that your OVER-SAVE email notifications are working correctly.</p>
                    <p>If you can read this message, your email setup is active and ready to receive monthly summaries!</p>
                    <hr style="margin: 25px 0; border:none; border-top:1px solid #ddd;">
                    <p style="font-size:12px; color:#777; text-align:center;">OVER-SAVE Budget Tracker<br>noreply@oversave.com</p>
                </div>
            </body>
            </html>
        """.formatted(firstName);

            helper.setText(html, true);
            mailSender.send(mimeMessage);
            log.info("✅ Test email sent to {}", email);
        } catch (Exception e) {
            log.error("❌ Failed to send test email to {}", email, e);
            throw new RuntimeException("Failed to send test email", e);
        }
    }

    @Override
    public void sendMonthlySummaryEmail(String email, String firstName, String summaryHtml) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject("Your Monthly OVER-SAVE Summary");

            // if no external summaryHtml provided, fallback to simple message
            String html = summaryHtml != null ? summaryHtml : """
            <html>
              <body style="font-family: Arial, sans-serif; padding:20px;">
                <h2>Hi %s 👋</h2>
                <p>Here’s your monthly OVER-SAVE summary!</p>
                <p>We’ll soon include budget, goals, and savings details here.</p>
                <hr><p style="font-size:12px;color:#777;">OVER-SAVE Budget Tracker</p>
              </body>
            </html>
        """.formatted(firstName);

            helper.setText(html, true);
            mailSender.send(mimeMessage);
            log.info("✅ Monthly summary email sent to {}", email);
        } catch (Exception e) {
            log.error("❌ Failed to send monthly summary to {}", email, e);
            throw new RuntimeException("Failed to send monthly summary email", e);
        }
    }

}