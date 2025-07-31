package com.example.session_demo.service;

/**
 * Service for sending SMS messages for patient verification
 */
public interface SmsService {
    
    /**
     * Send verification code via SMS
     * @param phoneNumber Phone number in international format
     * @param verificationCode The verification code to send
     */
    void sendVerificationCode(String phoneNumber, String verificationCode);
    
    /**
     * Send a general SMS message
     * @param phoneNumber Phone number in international format  
     * @param message The message to send
     */
    void sendMessage(String phoneNumber, String message);
} 