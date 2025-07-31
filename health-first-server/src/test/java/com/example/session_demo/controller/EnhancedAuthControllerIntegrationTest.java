package com.example.session_demo.controller;

import com.example.session_demo.dto.EnhancedLoginRequestDTO;
import com.example.session_demo.dto.TokenRefreshRequestDTO;
import com.example.session_demo.entity.ClinicAddress;
import com.example.session_demo.entity.Provider;
import com.example.session_demo.enums.ProviderSpecialization;
import com.example.session_demo.enums.VerificationStatus;
import com.example.session_demo.repository.ProviderRepository;
import com.example.session_demo.util.PasswordUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EnhancedAuthControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private PasswordUtil passwordUtil;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private Provider testProvider;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        objectMapper = new ObjectMapper();

        // Create test provider
        testProvider = new Provider();
        testProvider.setUuid(UUID.randomUUID());
        testProvider.setFirstName("John");
        testProvider.setLastName("Doe");
        testProvider.setEmail("john.doe@test.com");
        testProvider.setPhoneNumber("+1234567890");
        testProvider.setPasswordHash(passwordUtil.hashPassword("password123"));
        testProvider.setSpecialization(ProviderSpecialization.CARDIOLOGY);
        testProvider.setVerificationStatus(VerificationStatus.VERIFIED);
        testProvider.setIsActive(true);
        testProvider.setEmailVerified(true);
        testProvider.setFailedLoginAttempts(0);
        testProvider.setLoginCount(0);
        testProvider.setConcurrentSessions(0);
        
        // Add required fields for validation
        testProvider.setLicenseNumber("LIC123456");
        testProvider.setYearsOfExperience(5);
        
        // Create clinic address
        ClinicAddress clinicAddress = new ClinicAddress();
        clinicAddress.setStreet("123 Main St");
        clinicAddress.setCity("Test City");
        clinicAddress.setState("Test State");
        clinicAddress.setZip("12345");
        testProvider.setClinicAddress(clinicAddress);

        providerRepository.save(testProvider);
    }

    @Test
    void testSuccessfulLogin() throws Exception {
        // Arrange
        EnhancedLoginRequestDTO loginRequest = new EnhancedLoginRequestDTO();
        loginRequest.setIdentifier("john.doe@test.com");
        loginRequest.setPassword("password123");
        loginRequest.setRememberMe(false);
        loginRequest.setDeviceInfo("Chrome on Windows");

        // Act & Assert
        mockMvc.perform(post("/api/v1/provider/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
                .header("User-Agent", "Mozilla/5.0")
                .header("X-Forwarded-For", "192.168.1.1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.provider.email").value("john.doe@test.com"));
    }

    @Test
    void testLoginWithInvalidCredentials() throws Exception {
        // Arrange
        EnhancedLoginRequestDTO loginRequest = new EnhancedLoginRequestDTO();
        loginRequest.setIdentifier("john.doe@test.com");
        loginRequest.setPassword("wrongpassword");
        loginRequest.setRememberMe(false);

        // Act & Assert
        mockMvc.perform(post("/api/v1/provider/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
                .header("User-Agent", "Mozilla/5.0")
                .header("X-Forwarded-For", "192.168.1.1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("AUTHENTICATION_FAILED"));
    }

    @Test
    void testLoginWithNonExistentAccount() throws Exception {
        // Arrange
        EnhancedLoginRequestDTO loginRequest = new EnhancedLoginRequestDTO();
        loginRequest.setIdentifier("nonexistent@test.com");
        loginRequest.setPassword("password123");
        loginRequest.setRememberMe(false);

        // Act & Assert
        mockMvc.perform(post("/api/v1/provider/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
                .header("User-Agent", "Mozilla/5.0")
                .header("X-Forwarded-For", "192.168.1.1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("AUTHENTICATION_FAILED"));
    }

    @Test
    void testLoginWithInactiveAccount() throws Exception {
        // Arrange
        testProvider.setIsActive(false);
        providerRepository.save(testProvider);

        EnhancedLoginRequestDTO loginRequest = new EnhancedLoginRequestDTO();
        loginRequest.setIdentifier("john.doe@test.com");
        loginRequest.setPassword("password123");
        loginRequest.setRememberMe(false);

        // Act & Assert
        mockMvc.perform(post("/api/v1/provider/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
                .header("User-Agent", "Mozilla/5.0")
                .header("X-Forwarded-For", "192.168.1.1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_DISABLED"));
    }

    @Test
    void testLoginWithUnverifiedEmail() throws Exception {
        // Arrange
        testProvider.setEmailVerified(false);
        providerRepository.save(testProvider);

        EnhancedLoginRequestDTO loginRequest = new EnhancedLoginRequestDTO();
        loginRequest.setIdentifier("john.doe@test.com");
        loginRequest.setPassword("password123");
        loginRequest.setRememberMe(false);

        // Act & Assert
        mockMvc.perform(post("/api/v1/provider/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
                .header("User-Agent", "Mozilla/5.0")
                .header("X-Forwarded-For", "192.168.1.1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("EMAIL_NOT_VERIFIED"));
    }

    @Test
    void testLoginWithValidationErrors() throws Exception {
        // Arrange
        EnhancedLoginRequestDTO loginRequest = new EnhancedLoginRequestDTO();
        // Missing required fields

        // Act & Assert
        mockMvc.perform(post("/api/v1/provider/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
                .header("User-Agent", "Mozilla/5.0")
                .header("X-Forwarded-For", "192.168.1.1"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void testTokenRefreshWithInvalidToken() throws Exception {
        // Arrange
        TokenRefreshRequestDTO refreshRequest = new TokenRefreshRequestDTO();
        refreshRequest.setRefreshToken("invalidToken");

        // Act & Assert
        mockMvc.perform(post("/api/v1/provider/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest))
                .header("X-Forwarded-For", "192.168.1.1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("AUTHENTICATION_FAILED"));
    }

    @Test
    void testLogout() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/provider/logout")
                .param("refreshToken", "someToken")
                .header("X-Forwarded-For", "192.168.1.1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logout successful"));
    }

    @Test
    void testLogoutAll() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/provider/logout-all")
                .param("providerUuid", testProvider.getUuid().toString())
                .header("X-Forwarded-For", "192.168.1.1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logout from all sessions successful"));
    }
} 