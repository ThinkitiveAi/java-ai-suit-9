package com.example.session_demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${app.backend.url}")
    private String backendUrl;

    /**
     * Send email verification to provider
     */
    public void sendVerificationEmail(String toEmail, String firstName, String verificationToken) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Verify Your Healthcare Provider Account");
            
            String htmlContent = buildVerificationEmailContent(firstName, verificationToken);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Verification email sent successfully to: {}", toEmail);
            
        } catch (MessagingException e) {
            log.error("Failed to send verification email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send verification email", e);
        } catch (MailException e) {
            log.error("Mail service error when sending to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Mail service error", e);
        }
    }

    /**
     * Send welcome email after verification
     */
    public void sendWelcomeEmail(String toEmail, String firstName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Welcome to Healthcare Provider Network");
            
            String htmlContent = buildWelcomeEmailContent(firstName);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Welcome email sent successfully to: {}", toEmail);
            
        } catch (MessagingException | MailException e) {
            log.error("Failed to send welcome email to {}: {}", toEmail, e.getMessage());
            // Don't throw exception for welcome email failures
        }
    }

    /**
     * Send email verification to patient
     */
    public void sendPatientVerificationEmail(String toEmail, String firstName, String verificationToken) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Verify Your Patient Account");
            
            String htmlContent = buildPatientVerificationEmailContent(firstName, verificationToken);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Patient verification email sent successfully to: {}", toEmail);
            
        } catch (MessagingException e) {
            log.error("Failed to send patient verification email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send patient verification email", e);
        } catch (MailException e) {
            log.error("Mail service error when sending patient verification to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Mail service error", e);
        }
    }

    private String buildVerificationEmailContent(String firstName, String verificationToken) {
        String verificationUrl = backendUrl + "/api/v1/provider/verify-email?token=" + verificationToken;
        
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Verify Your Account</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #2c5aa0; color: white; padding: 20px; text-align: center; }
                    .content { background-color: #f9f9f9; padding: 30px; }
                    .button { display: inline-block; background-color: #2c5aa0; color: white; 
                             padding: 12px 30px; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .footer { text-align: center; color: #666; font-size: 12px; margin-top: 30px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Healthcare Provider Network</h1>
                    </div>
                    <div class="content">
                        <h2>Welcome Dr. %s!</h2>
                        <p>Thank you for registering with our Healthcare Provider Network. To complete your registration and activate your account, please verify your email address.</p>
                        
                        <p>Click the button below to verify your email:</p>
                        <p style="text-align: center;">
                            <a href="%s" class="button">Verify Email Address</a>
                        </p>
                        
                        <p>Or copy and paste this link into your browser:<br>
                        <a href="%s">%s</a></p>
                        
                        <p><strong>Important:</strong> This verification link will expire in 24 hours for security reasons.</p>
                        
                        <p>If you didn't create this account, please ignore this email.</p>
                        
                        <p>Best regards,<br>Healthcare Provider Network Team</p>
                    </div>
                    <div class="footer">
                        <p>© 2024 Healthcare Provider Network. All rights reserved.</p>
                        <p>This is an automated message, please do not reply to this email.</p>
                    </div>
                </div>
            </body>
            </html>
            """, firstName, verificationUrl, verificationUrl, verificationUrl);
    }

    private String buildWelcomeEmailContent(String firstName) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Welcome to Healthcare Provider Network</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #28a745; color: white; padding: 20px; text-align: center; }
                    .content { background-color: #f9f9f9; padding: 30px; }
                    .footer { text-align: center; color: #666; font-size: 12px; margin-top: 30px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🎉 Account Verified!</h1>
                    </div>
                    <div class="content">
                        <h2>Welcome to Healthcare Provider Network, Dr. %s!</h2>
                        <p>Congratulations! Your email has been successfully verified and your provider account is now active.</p>
                        
                        <p><strong>What's next?</strong></p>
                        <ul>
                            <li>Your account is pending verification by our admin team</li>
                            <li>You'll receive a notification once your credentials are verified</li>
                            <li>After verification, you'll have full access to our platform</li>
                        </ul>
                        
                        <p>If you have any questions, please don't hesitate to contact our support team.</p>
                        
                        <p>Thank you for joining our network!</p>
                        
                        <p>Best regards,<br>Healthcare Provider Network Team</p>
                    </div>
                    <div class="footer">
                        <p>© 2024 Healthcare Provider Network. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """, firstName);
    }

    private String buildPatientVerificationEmailContent(String firstName, String verificationToken) {
        String verificationUrl = backendUrl + "/api/v1/patient/verify-email?token=" + verificationToken;
        
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Verify Your Patient Account</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #2c5aa0; color: white; padding: 20px; text-align: center; }
                    .content { background-color: #f9f9f9; padding: 30px; }
                    .button { display: inline-block; background-color: #2c5aa0; color: white; 
                             padding: 12px 30px; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .footer { text-align: center; color: #666; font-size: 12px; margin-top: 30px; }
                    .security-note { background-color: #fff3cd; border: 1px solid #ffeaa7; 
                                   padding: 15px; border-radius: 5px; margin: 20px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🔐 Verify Your Patient Account</h1>
                    </div>
                    <div class="content">
                        <h2>Welcome %s!</h2>
                        <p>Thank you for registering as a patient with our HIPAA-compliant healthcare platform.</p>
                        
                        <p>To complete your registration and activate your account, please verify your email address by clicking the button below:</p>
                        
                        <p style="text-align: center;">
                            <a href="%s" class="button">Verify My Email Address</a>
                        </p>
                        
                        <div class="security-note">
                            <h3>🛡️ Your Privacy Matters</h3>
                            <ul>
                                <li>Your medical information is encrypted and HIPAA-compliant</li>
                                <li>You also need to verify your phone number via SMS</li>
                                <li>Both verifications are required to activate your account</li>
                            </ul>
                        </div>
                        
                        <p><strong>Next Steps:</strong></p>
                        <ol>
                            <li>Click the verification button above</li>
                            <li>Check your phone for an SMS verification code</li>
                            <li>Complete both verifications to access your account</li>
                        </ol>
                        
                        <p><small><strong>Security Note:</strong> This verification link will expire in 24 hours. If you didn't create this account, please ignore this email.</small></p>
                        
                        <p>If the button doesn't work, copy and paste this link into your browser:</p>
                        <p style="word-break: break-all; font-size: 12px; color: #666;">%s</p>
                        
                        <p>Best regards,<br>Healthcare Platform Team</p>
                    </div>
                    <div class="footer">
                        <p>© 2024 Healthcare Platform. All rights reserved.</p>
                        <p>This is an automated message for account verification.</p>
                    </div>
                </div>
            </body>
            </html>
            """, firstName, verificationUrl, verificationUrl);
    }
} 