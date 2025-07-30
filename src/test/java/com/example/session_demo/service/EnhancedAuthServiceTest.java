package com.example.session_demo.service;

import com.example.session_demo.dto.EnhancedLoginRequestDTO;
import com.example.session_demo.dto.TokenRefreshRequestDTO;
import com.example.session_demo.entity.LoginAttempt;
import com.example.session_demo.entity.Provider;
import com.example.session_demo.entity.RefreshToken;
import com.example.session_demo.enums.ProviderSpecialization;
import com.example.session_demo.enums.VerificationStatus;
import com.example.session_demo.repository.LoginAttemptRepository;
import com.example.session_demo.repository.ProviderRepository;
import com.example.session_demo.repository.RefreshTokenRepository;
import com.example.session_demo.util.EnhancedJwtUtil;
import com.example.session_demo.util.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnhancedAuthServiceTest {

    @Mock
    private ProviderRepository providerRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private LoginAttemptRepository loginAttemptRepository;

    @Mock
    private EnhancedJwtUtil enhancedJwtUtil;

    @Mock
    private PasswordUtil passwordUtil;

    @InjectMocks
    private EnhancedAuthService enhancedAuthService;

    private Provider testProvider;
    private EnhancedLoginRequestDTO loginRequest;
    private static final String TEST_IP = "192.168.1.1";
    private static final String TEST_USER_AGENT = "Mozilla/5.0";

    @BeforeEach
    void setUp() {
        // Set up test configuration
        ReflectionTestUtils.setField(enhancedAuthService, "maxLoginAttempts", 5);
        ReflectionTestUtils.setField(enhancedAuthService, "lockoutDuration", 1800000L);
        ReflectionTestUtils.setField(enhancedAuthService, "maxConcurrentSessions", 5);
        ReflectionTestUtils.setField(enhancedAuthService, "rateLimitWindow", 900000L);
        ReflectionTestUtils.setField(enhancedAuthService, "rateLimitMaxAttempts", 5);

        // Create test provider
        testProvider = new Provider();
        testProvider.setId(1L);
        testProvider.setUuid(UUID.randomUUID());
        testProvider.setFirstName("John");
        testProvider.setLastName("Doe");
        testProvider.setEmail("john.doe@test.com");
        testProvider.setPhoneNumber("+1234567890");
        testProvider.setPasswordHash("hashedPassword");
        testProvider.setSpecialization(ProviderSpecialization.CARDIOLOGY);
        testProvider.setVerificationStatus(VerificationStatus.VERIFIED);
        testProvider.setIsActive(true);
        testProvider.setEmailVerified(true);
        testProvider.setFailedLoginAttempts(0);
        testProvider.setLoginCount(0);
        testProvider.setConcurrentSessions(0);

        // Create test login request
        loginRequest = new EnhancedLoginRequestDTO();
        loginRequest.setIdentifier("john.doe@test.com");
        loginRequest.setPassword("password123");
        loginRequest.setRememberMe(false);
        loginRequest.setDeviceInfo("Chrome on Windows");
    }

    @Test
    void testSuccessfulLogin() {
        // Arrange
        when(loginAttemptRepository.countFailedAttemptsByIdentifier(anyString(), any(LocalDateTime.class)))
            .thenReturn(0L);
        when(loginAttemptRepository.countFailedAttemptsByIp(anyString(), any(LocalDateTime.class)))
            .thenReturn(0L);
        when(providerRepository.findByEmail(anyString()))
            .thenReturn(Optional.of(testProvider));
        when(passwordUtil.verifyPassword(anyString(), anyString()))
            .thenReturn(true);
        when(refreshTokenRepository.countActiveSessionsByProviderUuid(any(UUID.class), any(LocalDateTime.class)))
            .thenReturn(0L);
        when(enhancedJwtUtil.generateAccessToken(any(), anyString(), anyString(), anyString(), anyString(), anyString()))
            .thenReturn("accessToken");
        when(enhancedJwtUtil.generateRefreshToken(any(), anyString(), anyBoolean()))
            .thenReturn("refreshToken");
        when(enhancedJwtUtil.getRefreshTokenExpiration(anyBoolean()))
            .thenReturn(604800000L);
        when(passwordUtil.hashPassword(anyString()))
            .thenReturn("hashedRefreshToken");

        // Act
        var result = enhancedAuthService.login(loginRequest, TEST_IP, TEST_USER_AGENT);

        // Assert
        assertNotNull(result);
        assertEquals("accessToken", result.getAccessToken());
        assertEquals("refreshToken", result.getRefreshToken());
        assertEquals("Bearer", result.getTokenType());
        assertNotNull(result.getProvider());
        assertEquals(testProvider.getEmail(), result.getProvider().getEmail());

        // Verify interactions
        verify(providerRepository).save(any(Provider.class));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
        verify(loginAttemptRepository).save(any(LoginAttempt.class));
    }

    @Test
    void testLoginWithInvalidCredentials() {
        // Arrange
        when(loginAttemptRepository.countFailedAttemptsByIdentifier(anyString(), any(LocalDateTime.class)))
            .thenReturn(0L);
        when(loginAttemptRepository.countFailedAttemptsByIp(anyString(), any(LocalDateTime.class)))
            .thenReturn(0L);
        when(providerRepository.findByEmail(anyString()))
            .thenReturn(Optional.of(testProvider));
        when(passwordUtil.verifyPassword(anyString(), anyString()))
            .thenReturn(false);

        // Act & Assert
        assertThrows(EnhancedAuthService.AuthenticationException.class, () -> {
            enhancedAuthService.login(loginRequest, TEST_IP, TEST_USER_AGENT);
        });

        // Verify failed attempt was logged
        verify(loginAttemptRepository).save(any(LoginAttempt.class));
        verify(providerRepository).save(any(Provider.class));
    }

    @Test
    void testLoginWithAccountLocked() {
        // Arrange
        testProvider.setLockedUntil(LocalDateTime.now().plusMinutes(30));
        when(loginAttemptRepository.countFailedAttemptsByIdentifier(anyString(), any(LocalDateTime.class)))
            .thenReturn(0L);
        when(loginAttemptRepository.countFailedAttemptsByIp(anyString(), any(LocalDateTime.class)))
            .thenReturn(0L);
        when(providerRepository.findByEmail(anyString()))
            .thenReturn(Optional.of(testProvider));

        // Act & Assert
        assertThrows(EnhancedAuthService.AuthenticationException.class, () -> {
            enhancedAuthService.login(loginRequest, TEST_IP, TEST_USER_AGENT);
        });

        // Verify locked attempt was logged
        verify(loginAttemptRepository).save(any(LoginAttempt.class));
    }

    @Test
    void testLoginWithRateLimitExceeded() {
        // Arrange
        when(loginAttemptRepository.countFailedAttemptsByIdentifier(anyString(), any(LocalDateTime.class)))
            .thenReturn(5L); // Max attempts reached

        // Act & Assert
        assertThrows(EnhancedAuthService.AuthenticationException.class, () -> {
            enhancedAuthService.login(loginRequest, TEST_IP, TEST_USER_AGENT);
        });

        // Verify no provider lookup occurred
        verify(providerRepository, never()).findByEmail(anyString());
    }

    @Test
    void testLoginWithInactiveAccount() {
        // Arrange
        testProvider.setIsActive(false);
        when(loginAttemptRepository.countFailedAttemptsByIdentifier(anyString(), any(LocalDateTime.class)))
            .thenReturn(0L);
        when(loginAttemptRepository.countFailedAttemptsByIp(anyString(), any(LocalDateTime.class)))
            .thenReturn(0L);
        when(providerRepository.findByEmail(anyString()))
            .thenReturn(Optional.of(testProvider));

        // Act & Assert
        assertThrows(EnhancedAuthService.AuthenticationException.class, () -> {
            enhancedAuthService.login(loginRequest, TEST_IP, TEST_USER_AGENT);
        });

        // Verify disabled account attempt was logged
        verify(loginAttemptRepository).save(any(LoginAttempt.class));
    }

    @Test
    void testLoginWithUnverifiedEmail() {
        // Arrange
        testProvider.setEmailVerified(false);
        when(loginAttemptRepository.countFailedAttemptsByIdentifier(anyString(), any(LocalDateTime.class)))
            .thenReturn(0L);
        when(loginAttemptRepository.countFailedAttemptsByIp(anyString(), any(LocalDateTime.class)))
            .thenReturn(0L);
        when(providerRepository.findByEmail(anyString()))
            .thenReturn(Optional.of(testProvider));

        // Act & Assert
        assertThrows(EnhancedAuthService.AuthenticationException.class, () -> {
            enhancedAuthService.login(loginRequest, TEST_IP, TEST_USER_AGENT);
        });

        // Verify unverified email attempt was logged
        verify(loginAttemptRepository).save(any(LoginAttempt.class));
    }

    @Test
    void testSuccessfulTokenRefresh() {
        // Arrange
        TokenRefreshRequestDTO refreshRequest = new TokenRefreshRequestDTO();
        refreshRequest.setRefreshToken("validRefreshToken");

        when(enhancedJwtUtil.isRefreshToken(anyString())).thenReturn(true);
        when(enhancedJwtUtil.validateToken(anyString())).thenReturn(true);
        when(enhancedJwtUtil.extractUuid(anyString())).thenReturn(testProvider.getUuid());
        when(enhancedJwtUtil.extractEmail(anyString())).thenReturn(testProvider.getEmail());
        when(providerRepository.findByUuid(any(UUID.class))).thenReturn(Optional.of(testProvider));
        when(passwordUtil.hashPassword(anyString())).thenReturn("hashedToken");
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(new RefreshToken()));
        when(enhancedJwtUtil.generateAccessToken(any(), anyString(), anyString(), anyString(), anyString(), anyString()))
            .thenReturn("newAccessToken");
        when(enhancedJwtUtil.generateRefreshToken(any(), anyString(), anyBoolean()))
            .thenReturn("newRefreshToken");
        when(enhancedJwtUtil.getRefreshTokenExpiration(anyBoolean())).thenReturn(604800000L);

        // Act
        var result = enhancedAuthService.refreshToken(refreshRequest, TEST_IP);

        // Assert
        assertNotNull(result);
        assertEquals("newAccessToken", result.getAccessToken());
        assertEquals("newRefreshToken", result.getRefreshToken());
        assertEquals("Bearer", result.getTokenType());

        // Verify old token was revoked and new one was saved
        verify(refreshTokenRepository).revokeTokenByHash(anyString());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void testTokenRefreshWithInvalidToken() {
        // Arrange
        TokenRefreshRequestDTO refreshRequest = new TokenRefreshRequestDTO();
        refreshRequest.setRefreshToken("invalidToken");

        when(enhancedJwtUtil.isRefreshToken(anyString())).thenReturn(false);

        // Act & Assert
        assertThrows(EnhancedAuthService.AuthenticationException.class, () -> {
            enhancedAuthService.refreshToken(refreshRequest, TEST_IP);
        });
    }

    @Test
    void testLogout() {
        // Arrange
        String refreshToken = "validRefreshToken";
        when(enhancedJwtUtil.isRefreshToken(anyString())).thenReturn(true);
        when(enhancedJwtUtil.validateToken(anyString())).thenReturn(true);
        when(passwordUtil.hashPassword(anyString())).thenReturn("hashedToken");

        // Act
        assertDoesNotThrow(() -> {
            enhancedAuthService.logout(refreshToken, TEST_IP);
        });

        // Verify token was revoked
        verify(refreshTokenRepository).revokeTokenByHash(anyString());
    }

    @Test
    void testLogoutAll() {
        // Arrange
        UUID providerUuid = UUID.randomUUID();

        // Act
        assertDoesNotThrow(() -> {
            enhancedAuthService.logoutAll(providerUuid);
        });

        // Verify all tokens were revoked
        verify(refreshTokenRepository).revokeAllTokensByProviderUuid(providerUuid);
    }
} 