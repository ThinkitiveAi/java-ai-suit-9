package com.example.session_demo.service;

import com.example.session_demo.dto.ProviderRegisterDTO;
import com.example.session_demo.dto.ProviderResponseDTO;
import com.example.session_demo.entity.Provider;
import com.example.session_demo.enums.ProviderSpecialization;
import com.example.session_demo.enums.VerificationStatus;
import com.example.session_demo.repository.ProviderRepository;
import com.example.session_demo.util.PasswordUtil;
import com.example.session_demo.util.PhoneNumberUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProviderServiceTest {

    @Mock
    private ProviderRepository providerRepository;

    @Mock
    private PasswordUtil passwordUtil;

    @Mock
    private PhoneNumberUtil phoneNumberUtil;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private ProviderService providerService;

    private ProviderRegisterDTO validRegisterDTO;
    private Provider mockProvider;

    @BeforeEach
    void setUp() {
        // Setup valid registration DTO
        validRegisterDTO = new ProviderRegisterDTO();
        validRegisterDTO.setFirstName("John");
        validRegisterDTO.setLastName("Doe");
        validRegisterDTO.setEmail("john.doe@clinic.com");
        validRegisterDTO.setPhoneNumber("+1234567890");
        validRegisterDTO.setPassword("SecurePassword123!");
        validRegisterDTO.setConfirmPassword("SecurePassword123!");
        validRegisterDTO.setSpecialization(ProviderSpecialization.CARDIOLOGY);
        validRegisterDTO.setLicenseNumber("MD123456789");
        validRegisterDTO.setYearsOfExperience(10);

        ProviderRegisterDTO.ClinicAddressDTO addressDTO = new ProviderRegisterDTO.ClinicAddressDTO();
        addressDTO.setStreet("123 Medical Center Dr");
        addressDTO.setCity("New York");
        addressDTO.setState("NY");
        addressDTO.setZip("10001");
        validRegisterDTO.setClinicAddress(addressDTO);

        // Setup mock provider
        mockProvider = new Provider();
        mockProvider.setId(1L);
        mockProvider.setUuid(UUID.randomUUID());
        mockProvider.setFirstName("John");
        mockProvider.setLastName("Doe");
        mockProvider.setEmail("john.doe@clinic.com");
        mockProvider.setPhoneNumber("+1234567890");
        mockProvider.setSpecialization(ProviderSpecialization.CARDIOLOGY);
        mockProvider.setVerificationStatus(VerificationStatus.PENDING);
        mockProvider.setEmailVerified(false);
    }

    @Test
    void registerProvider_ValidData_Success() {
        // Arrange
        when(passwordUtil.passwordsMatch(anyString(), anyString())).thenReturn(true);
        when(passwordUtil.isValidPassword(anyString())).thenReturn(true);
        when(phoneNumberUtil.isValidPhoneNumber(anyString())).thenReturn(true);
        try {
            when(phoneNumberUtil.normalizePhoneNumber(anyString())).thenReturn("+1234567890");
        } catch (Exception e) {
            // This won't happen in our test, but needed for compilation
        }
        when(passwordUtil.hashPassword(anyString())).thenReturn("hashedPassword");
        when(passwordUtil.generateSecureToken()).thenReturn("verificationToken");
        
        when(providerRepository.existsByEmail(anyString())).thenReturn(false);
        when(providerRepository.existsByPhoneNumber(anyString())).thenReturn(false);
        when(providerRepository.existsByLicenseNumber(anyString())).thenReturn(false);
        when(providerRepository.save(any(Provider.class))).thenReturn(mockProvider);

        // Act
        ProviderResponseDTO result = providerService.registerProvider(validRegisterDTO, "127.0.0.1");

        // Assert
        assertNotNull(result);
        assertEquals("john.doe@clinic.com", result.getEmail());
        assertEquals(VerificationStatus.PENDING, result.getVerificationStatus());
        assertFalse(result.getEmailVerified());

        verify(providerRepository).save(any(Provider.class));
        verify(emailService).sendVerificationEmail(anyString(), anyString(), anyString());
    }

    @Test
    void registerProvider_DuplicateEmail_ThrowsConflictException() {
        // Arrange
        when(passwordUtil.passwordsMatch(anyString(), anyString())).thenReturn(true);
        when(passwordUtil.isValidPassword(anyString())).thenReturn(true);
        when(phoneNumberUtil.isValidPhoneNumber(anyString())).thenReturn(true);
        when(providerRepository.existsByEmail(anyString())).thenReturn(true);

        // Act & Assert
        assertThrows(ProviderService.ConflictException.class, () -> {
            providerService.registerProvider(validRegisterDTO, "127.0.0.1");
        });

        verify(providerRepository, never()).save(any(Provider.class));
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString(), anyString());
    }

    @Test
    void registerProvider_PasswordMismatch_ThrowsValidationException() {
        // Arrange
        when(passwordUtil.passwordsMatch(anyString(), anyString())).thenReturn(false);

        // Act & Assert
        assertThrows(ProviderService.ValidationException.class, () -> {
            providerService.registerProvider(validRegisterDTO, "127.0.0.1");
        });

        verify(providerRepository, never()).save(any(Provider.class));
    }

    @Test
    void registerProvider_InvalidPhoneNumber_ThrowsValidationException() {
        // Arrange
        when(passwordUtil.passwordsMatch(anyString(), anyString())).thenReturn(true);
        when(passwordUtil.isValidPassword(anyString())).thenReturn(true);
        when(phoneNumberUtil.isValidPhoneNumber(anyString())).thenReturn(false);

        // Act & Assert
        assertThrows(ProviderService.ValidationException.class, () -> {
            providerService.registerProvider(validRegisterDTO, "127.0.0.1");
        });

        verify(providerRepository, never()).save(any(Provider.class));
    }

    @Test
    void verifyEmail_ValidToken_Success() {
        // Arrange
        String token = "validToken";
        mockProvider.setEmailVerificationToken(token);
        mockProvider.setEmailVerified(false);
        
        when(providerRepository.findByEmailVerificationToken(token)).thenReturn(Optional.of(mockProvider));
        when(providerRepository.save(any(Provider.class))).thenReturn(mockProvider);

        // Act
        boolean result = providerService.verifyEmail(token);

        // Assert
        assertTrue(result);
        verify(providerRepository).save(any(Provider.class));
        verify(emailService).sendWelcomeEmail(anyString(), anyString());
    }

    @Test
    void verifyEmail_InvalidToken_ReturnsFalse() {
        // Arrange
        String token = "invalidToken";
        when(providerRepository.findByEmailVerificationToken(token)).thenReturn(Optional.empty());

        // Act
        boolean result = providerService.verifyEmail(token);

        // Assert
        assertFalse(result);
        verify(providerRepository, never()).save(any(Provider.class));
        verify(emailService, never()).sendWelcomeEmail(anyString(), anyString());
    }

    @Test
    void getProviderById_ExistingProvider_ReturnsProvider() {
        // Arrange
        UUID providerId = UUID.randomUUID();
        when(providerRepository.findById(providerId)).thenReturn(Optional.of(mockProvider));

        // Act
        Optional<ProviderResponseDTO> result = providerService.getProviderById(providerId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals("john.doe@clinic.com", result.get().getEmail());
    }

    @Test
    void getProviderById_NonExistingProvider_ReturnsEmpty() {
        // Arrange
        UUID providerId = UUID.randomUUID();
        when(providerRepository.findById(providerId)).thenReturn(Optional.empty());

        // Act
        Optional<ProviderResponseDTO> result = providerService.getProviderById(providerId);

        // Assert
        assertFalse(result.isPresent());
    }
} 