package com.example.session_demo.service;

import com.example.session_demo.repository.LoginAttemptRepository;
import com.example.session_demo.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionCleanupService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final LoginAttemptRepository loginAttemptRepository;

    @Value("${session.cleanup.login-attempts-retention-days:90}")
    private int loginAttemptsRetentionDays;

    @Value("${session.cleanup.refresh-tokens-retention-days:30}")
    private int refreshTokensRetentionDays;

    /**
     * Clean up expired refresh tokens
     * Runs every hour
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    @Transactional
    public void cleanupExpiredRefreshTokens() {
        try {
            log.info("Starting cleanup of expired refresh tokens");
            
            LocalDateTime now = LocalDateTime.now();
            int deletedCount = refreshTokenRepository.findExpiredTokens(now).size();
            
            refreshTokenRepository.deleteExpiredTokens(now);
            
            log.info("Cleanup completed. Deleted {} expired refresh tokens", deletedCount);
            
        } catch (Exception e) {
            log.error("Error during refresh token cleanup: {}", e.getMessage(), e);
        }
    }

    /**
     * Clean up old login attempts for audit trail maintenance
     * Runs daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    @Transactional
    public void cleanupOldLoginAttempts() {
        try {
            log.info("Starting cleanup of old login attempts");
            
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(loginAttemptsRetentionDays);
            loginAttemptRepository.deleteOldAttempts(cutoffDate);
            
            log.info("Cleanup completed. Deleted login attempts older than {} days", loginAttemptsRetentionDays);
            
        } catch (Exception e) {
            log.error("Error during login attempts cleanup: {}", e.getMessage(), e);
        }
    }

    /**
     * Clean up old refresh tokens for security maintenance
     * Runs daily at 3 AM
     */
    @Scheduled(cron = "0 0 3 * * ?") // Daily at 3 AM
    @Transactional
    public void cleanupOldRefreshTokens() {
        try {
            log.info("Starting cleanup of old refresh tokens");
            
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(refreshTokensRetentionDays);
            refreshTokenRepository.deleteExpiredTokens(cutoffDate);
            
            log.info("Cleanup completed. Deleted refresh tokens older than {} days", refreshTokensRetentionDays);
            
        } catch (Exception e) {
            log.error("Error during old refresh tokens cleanup: {}", e.getMessage(), e);
        }
    }

    /**
     * Manual cleanup method for immediate execution
     */
    @Transactional
    public void performManualCleanup() {
        log.info("Performing manual cleanup");
        cleanupExpiredRefreshTokens();
        cleanupOldLoginAttempts();
        cleanupOldRefreshTokens();
        log.info("Manual cleanup completed");
    }
} 