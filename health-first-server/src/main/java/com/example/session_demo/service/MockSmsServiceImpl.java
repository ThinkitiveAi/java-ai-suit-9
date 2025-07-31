package com.example.session_demo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Mock SMS service implementation for development/testing
 * Logs SMS messages to console instead of sending real SMS
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "sms.service.provider", havingValue = "mock", matchIfMissing = true)
public class MockSmsServiceImpl implements SmsService {

    @Override
    public void sendVerificationCode(String phoneNumber, String verificationCode) {
        log.info("=== MOCK SMS SERVICE ===");
        log.info("TO: {}", phoneNumber);
        log.info("MESSAGE: Your healthcare verification code is: {}", verificationCode);
        log.info("This code expires in 5 minutes.");
        log.info("=======================");
        
        // Simulate SMS sending delay
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        log.info("SMS verification code sent successfully to: {}", phoneNumber);
    }

    @Override
    public void sendMessage(String phoneNumber, String message) {
        log.info("=== MOCK SMS SERVICE ===");
        log.info("TO: {}", phoneNumber);
        log.info("MESSAGE: {}", message);
        log.info("=======================");
        
        // Simulate SMS sending delay
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        log.info("SMS message sent successfully to: {}", phoneNumber);
    }
} 