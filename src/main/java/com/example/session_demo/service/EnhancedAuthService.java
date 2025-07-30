package com.example.session_demo.service;

import com.example.session_demo.dto.EnhancedLoginRequestDTO;
import com.example.session_demo.dto.EnhancedLoginResponseDTO;
import com.example.session_demo.dto.TokenRefreshRequestDTO;
import com.example.session_demo.dto.TokenRefreshResponseDTO;
import com.example.session_demo.entity.LoginAttempt;
import com.example.session_demo.entity.Provider;
import com.example.session_demo.entity.RefreshToken;
import com.example.session_demo.enums.VerificationStatus;
import com.example.session_demo.repository.LoginAttemptRepository;
import com.example.session_demo.repository.ProviderRepository;
import com.example.session_demo.repository.RefreshTokenRepository;
import com.example.session_demo.util.EnhancedJwtUtil;
import com.example.session_demo.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnhancedAuthService {

    private final ProviderRepository providerRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final EnhancedJwtUtil enhancedJwtUtil;
    private final PasswordUtil passwordUtil;

    @Value("${security.max-login-attempts:5}")
    private int maxLoginAttempts;

    @Value("${security.account-lockout-duration:1800000}") // 30 minutes in milliseconds
    private long lockoutDuration;

    @Value("${security.max-concurrent-sessions:5}")
    private int maxConcurrentSessions;

    @Value("${security.rate-limit-window:900000}") // 15 minutes in milliseconds
    private long rateLimitWindow;

    @Value("${security.rate-limit-max-attempts:5}")
    private int rateLimitMaxAttempts;

    @Transactional
    public EnhancedLoginResponseDTO login(EnhancedLoginRequestDTO loginRequest, String ipAddress, String userAgent) {
        log.info("Login attempt for identifier: {} from IP: {}", loginRequest.getIdentifier(), ipAddress);

        // 1. Input validation
        validateLoginRequest(loginRequest);

        // 2. Rate limiting check
        checkRateLimiting(loginRequest.getIdentifier(), ipAddress);

        // 3. Find provider by identifier (email or phone)
        Optional<Provider> providerOpt = findProviderByIdentifier(loginRequest.getIdentifier());
        
        if (providerOpt.isEmpty()) {
            logFailedAttempt(null, loginRequest.getIdentifier(), ipAddress, userAgent, 
                LoginAttempt.AttemptType.FAILED, LoginAttempt.FailureReason.ACCOUNT_NOT_FOUND);
            throw new AuthenticationException("Invalid identifier or password");
        }

        Provider provider = providerOpt.get();

        // 4. Account verification checks
        validateAccountStatus(provider, loginRequest.getIdentifier(), ipAddress, userAgent);

        // 5. Check account lockout
        checkAccountLockout(provider, loginRequest.getIdentifier(), ipAddress, userAgent);

        // 6. Password verification
        if (!passwordUtil.verifyPassword(loginRequest.getPassword(), provider.getPasswordHash())) {
            handleFailedLogin(provider, loginRequest.getIdentifier(), ipAddress, userAgent);
            throw new AuthenticationException("Invalid identifier or password");
        }

        // 7. Check concurrent sessions
        checkConcurrentSessions(provider);

        // 8. Generate tokens
        String accessToken = enhancedJwtUtil.generateAccessToken(
            provider.getUuid(),
            provider.getEmail(),
            provider.getFirstName(),
            provider.getLastName(),
            provider.getSpecialization().name(),
            provider.getVerificationStatus().name()
        );

        String refreshToken = enhancedJwtUtil.generateRefreshToken(
            provider.getUuid(),
            provider.getEmail(),
            loginRequest.getRememberMe()
        );

        // 9. Store refresh token
        RefreshToken refreshTokenEntity = new RefreshToken();
        refreshTokenEntity.setProvider(provider);
        refreshTokenEntity.setTokenHash(passwordUtil.hashPassword(refreshToken)); // Hash the refresh token
        refreshTokenEntity.setExpiresAt(LocalDateTime.now().plusSeconds(
            enhancedJwtUtil.getRefreshTokenExpiration(loginRequest.getRememberMe()) / 1000
        ));
        refreshTokenEntity.setDeviceInfo(loginRequest.getDeviceInfo());
        refreshTokenEntity.setIpAddress(ipAddress);
        refreshTokenEntity.setUserAgent(userAgent);
        refreshTokenRepository.save(refreshTokenEntity);

        // 10. Update provider login statistics
        updateProviderLoginStats(provider);

        // 11. Log successful login
        logSuccessfulAttempt(provider, loginRequest.getIdentifier(), ipAddress, userAgent);

        log.info("Login successful for provider: {}", provider.getUuid());

        return buildLoginResponse(accessToken, refreshToken, provider, loginRequest.getRememberMe());
    }

    @Transactional
    public TokenRefreshResponseDTO refreshToken(TokenRefreshRequestDTO refreshRequest, String ipAddress) {
        log.info("Token refresh attempt from IP: {}", ipAddress);

        try {
            // 1. Validate refresh token
            if (!enhancedJwtUtil.isRefreshToken(refreshRequest.getRefreshToken())) {
                throw new AuthenticationException("Invalid refresh token");
            }

            if (!enhancedJwtUtil.validateToken(refreshRequest.getRefreshToken())) {
                throw new AuthenticationException("Refresh token expired or invalid");
            }

            // 2. Extract provider info from token
            UUID providerUuid = enhancedJwtUtil.extractUuid(refreshRequest.getRefreshToken());
            String email = enhancedJwtUtil.extractEmail(refreshRequest.getRefreshToken());

            // 3. Find provider
            Optional<Provider> providerOpt = providerRepository.findByUuid(providerUuid);
            if (providerOpt.isEmpty()) {
                throw new AuthenticationException("Provider not found");
            }

            Provider provider = providerOpt.get();

            // 4. Verify refresh token in database
            String tokenHash = passwordUtil.hashPassword(refreshRequest.getRefreshToken());
            Optional<RefreshToken> refreshTokenOpt = refreshTokenRepository.findByTokenHash(tokenHash);
            
            if (refreshTokenOpt.isEmpty() || !refreshTokenOpt.get().isValid()) {
                throw new AuthenticationException("Refresh token not found or revoked");
            }

            RefreshToken refreshTokenEntity = refreshTokenOpt.get();

            // 5. Check if provider is still active
            if (!provider.getIsActive() || !provider.getEmailVerified()) {
                throw new AuthenticationException("Account is not active");
            }

            // 6. Generate new tokens
            String newAccessToken = enhancedJwtUtil.generateAccessToken(
                provider.getUuid(),
                provider.getEmail(),
                provider.getFirstName(),
                provider.getLastName(),
                provider.getSpecialization().name(),
                provider.getVerificationStatus().name()
            );

            String newRefreshToken = enhancedJwtUtil.generateRefreshToken(
                provider.getUuid(),
                provider.getEmail(),
                false // Default to false for refresh
            );

            // 7. Revoke old refresh token and store new one
            refreshTokenRepository.revokeTokenByHash(tokenHash);
            
            RefreshToken newRefreshTokenEntity = new RefreshToken();
            newRefreshTokenEntity.setProvider(provider);
            newRefreshTokenEntity.setTokenHash(passwordUtil.hashPassword(newRefreshToken));
            newRefreshTokenEntity.setExpiresAt(LocalDateTime.now().plusSeconds(
                enhancedJwtUtil.getRefreshTokenExpiration(false) / 1000
            ));
            newRefreshTokenEntity.setDeviceInfo(refreshTokenEntity.getDeviceInfo());
            newRefreshTokenEntity.setIpAddress(ipAddress);
            newRefreshTokenEntity.setUserAgent(refreshTokenEntity.getUserAgent());
            refreshTokenRepository.save(newRefreshTokenEntity);

            log.info("Token refresh successful for provider: {}", provider.getUuid());

            return TokenRefreshResponseDTO.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .expiresIn(enhancedJwtUtil.getAccessTokenExpiration())
                    .refreshExpiresIn(enhancedJwtUtil.getRefreshTokenExpiration(false))
                    .tokenType("Bearer")
                    .build();

        } catch (Exception e) {
            log.warn("Token refresh failed: {}", e.getMessage());
            throw new AuthenticationException("Token refresh failed");
        }
    }

    @Transactional
    public void logout(String refreshToken, String ipAddress) {
        log.info("Logout attempt from IP: {}", ipAddress);

        try {
            if (enhancedJwtUtil.isRefreshToken(refreshToken) && enhancedJwtUtil.validateToken(refreshToken)) {
                String tokenHash = passwordUtil.hashPassword(refreshToken);
                refreshTokenRepository.revokeTokenByHash(tokenHash);
                log.info("Token revoked successfully");
            }
        } catch (Exception e) {
            log.warn("Error during logout: {}", e.getMessage());
        }
    }

    @Transactional
    public void logoutAll(UUID providerUuid) {
        log.info("Logout all sessions for provider: {}", providerUuid);
        refreshTokenRepository.revokeAllTokensByProviderUuid(providerUuid);
    }

    // Private helper methods

    private void validateLoginRequest(EnhancedLoginRequestDTO loginRequest) {
        if (loginRequest.getIdentifier() == null || loginRequest.getIdentifier().trim().isEmpty()) {
            throw new AuthenticationException("Identifier is required");
        }
        if (loginRequest.getPassword() == null || loginRequest.getPassword().trim().isEmpty()) {
            throw new AuthenticationException("Password is required");
        }
    }

    private void checkRateLimiting(String identifier, String ipAddress) {
        LocalDateTime since = LocalDateTime.now().minusSeconds(rateLimitWindow / 1000);
        
        long attemptsByIdentifier = loginAttemptRepository.countFailedAttemptsByIdentifier(identifier, since);
        long attemptsByIp = loginAttemptRepository.countFailedAttemptsByIp(ipAddress, since);
        
        if (attemptsByIdentifier >= rateLimitMaxAttempts) {
            log.warn("Rate limit exceeded for identifier: {}", identifier);
            throw new AuthenticationException("Too many login attempts. Please try again later.");
        }
        
        if (attemptsByIp >= rateLimitMaxAttempts * 2) { // Higher limit for IP-based
            log.warn("Rate limit exceeded for IP: {}", ipAddress);
            throw new AuthenticationException("Too many login attempts from this IP. Please try again later.");
        }
    }

    private Optional<Provider> findProviderByIdentifier(String identifier) {
        if (identifier.contains("@")) {
            return providerRepository.findByEmail(identifier.toLowerCase().trim());
        } else {
            // For phone numbers, you might need to implement findByPhoneNumber
            return providerRepository.findByEmail(identifier); // Fallback for now
        }
    }

    private void validateAccountStatus(Provider provider, String identifier, String ipAddress, String userAgent) {
        if (!provider.getIsActive()) {
            logFailedAttempt(provider, identifier, ipAddress, userAgent, 
                LoginAttempt.AttemptType.ACCOUNT_DISABLED, LoginAttempt.FailureReason.ACCOUNT_DISABLED);
            throw new AuthenticationException("Account is deactivated. Please contact support.");
        }

        if (!provider.getEmailVerified()) {
            logFailedAttempt(provider, identifier, ipAddress, userAgent, 
                LoginAttempt.AttemptType.EMAIL_NOT_VERIFIED, LoginAttempt.FailureReason.EMAIL_NOT_VERIFIED);
            throw new AuthenticationException("Email not verified. Please verify your email before logging in.");
        }
    }

    private void checkAccountLockout(Provider provider, String identifier, String ipAddress, String userAgent) {
        if (provider.getLockedUntil() != null && LocalDateTime.now().isBefore(provider.getLockedUntil())) {
            logFailedAttempt(provider, identifier, ipAddress, userAgent, 
                LoginAttempt.AttemptType.LOCKED, LoginAttempt.FailureReason.ACCOUNT_LOCKED);
            throw new AuthenticationException("Account temporarily locked due to multiple failed attempts");
        }
    }

    private void handleFailedLogin(Provider provider, String identifier, String ipAddress, String userAgent) {
        provider.setFailedLoginAttempts(provider.getFailedLoginAttempts() + 1);
        
        if (provider.getFailedLoginAttempts() >= maxLoginAttempts) {
            // Lock account
            provider.setLockedUntil(LocalDateTime.now().plusSeconds(lockoutDuration / 1000));
            logFailedAttempt(provider, identifier, ipAddress, userAgent, 
                LoginAttempt.AttemptType.LOCKED, LoginAttempt.FailureReason.TOO_MANY_ATTEMPTS);
        } else {
            logFailedAttempt(provider, identifier, ipAddress, userAgent, 
                LoginAttempt.AttemptType.FAILED, LoginAttempt.FailureReason.INVALID_PASSWORD);
        }
        
        providerRepository.save(provider);
    }

    private void checkConcurrentSessions(Provider provider) {
        long activeSessions = refreshTokenRepository.countActiveSessionsByProviderUuid(
            provider.getUuid(), LocalDateTime.now());
        
        if (activeSessions >= maxConcurrentSessions) {
            log.warn("Too many concurrent sessions for provider: {}", provider.getUuid());
            throw new AuthenticationException("Too many active sessions. Please logout from other devices.");
        }
    }

    private void updateProviderLoginStats(Provider provider) {
        provider.setLastLogin(LocalDateTime.now());
        provider.setLoginCount(provider.getLoginCount() + 1);
        provider.setFailedLoginAttempts(0); // Reset failed attempts on successful login
        provider.setLockedUntil(null); // Remove lockout
        provider.setConcurrentSessions((int) refreshTokenRepository.countActiveSessionsByProviderUuid(
            provider.getUuid(), LocalDateTime.now()) + 1);
        providerRepository.save(provider);
    }

    private void logSuccessfulAttempt(Provider provider, String identifier, String ipAddress, String userAgent) {
        LoginAttempt loginAttempt = new LoginAttempt();
        loginAttempt.setProvider(provider);
        loginAttempt.setIdentifier(identifier);
        loginAttempt.setIpAddress(ipAddress);
        loginAttempt.setUserAgent(userAgent);
        loginAttempt.setAttemptType(LoginAttempt.AttemptType.SUCCESS);
        loginAttemptRepository.save(loginAttempt);
    }

    private void logFailedAttempt(Provider provider, String identifier, String ipAddress, String userAgent, 
                                 LoginAttempt.AttemptType attemptType, LoginAttempt.FailureReason failureReason) {
        LoginAttempt loginAttempt = new LoginAttempt();
        loginAttempt.setProvider(provider);
        loginAttempt.setIdentifier(identifier);
        loginAttempt.setIpAddress(ipAddress);
        loginAttempt.setUserAgent(userAgent);
        loginAttempt.setAttemptType(attemptType);
        loginAttempt.setFailureReason(failureReason.name());
        loginAttemptRepository.save(loginAttempt);
    }

    private EnhancedLoginResponseDTO buildLoginResponse(String accessToken, String refreshToken, Provider provider, boolean rememberMe) {
        return EnhancedLoginResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(enhancedJwtUtil.getAccessTokenExpiration())
                .refreshExpiresIn(enhancedJwtUtil.getRefreshTokenExpiration(rememberMe))
                .tokenType("Bearer")
                .provider(EnhancedLoginResponseDTO.ProviderInfo.builder()
                        .id(provider.getId())
                        .uuid(provider.getUuid())
                        .firstName(provider.getFirstName())
                        .lastName(provider.getLastName())
                        .email(provider.getEmail())
                        .phoneNumber(provider.getPhoneNumber())
                        .specialization(provider.getSpecialization().name())
                        .verificationStatus(provider.getVerificationStatus().name())
                        .isActive(provider.getIsActive())
                        .lastLogin(provider.getLastLogin())
                        .loginCount(provider.getLoginCount())
                        .build())
                .build();
    }

    public static class AuthenticationException extends RuntimeException {
        public AuthenticationException(String message) {
            super(message);
        }
    }
} 