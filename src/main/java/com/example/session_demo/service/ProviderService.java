package com.example.session_demo.service;

import com.example.session_demo.dto.ProviderRegisterDTO;
import com.example.session_demo.dto.ProviderResponseDTO;
import com.example.session_demo.dto.ProviderUpdateDTO;
import com.example.session_demo.entity.ClinicAddress;
import com.example.session_demo.entity.Provider;
import com.example.session_demo.enums.VerificationStatus;
import com.example.session_demo.repository.ProviderRepository;
import com.example.session_demo.util.PasswordUtil;
import com.example.session_demo.util.PhoneNumberUtil;
import com.google.i18n.phonenumbers.NumberParseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProviderService {

    private final ProviderRepository providerRepository;
    private final PasswordUtil passwordUtil;
    private final PhoneNumberUtil phoneNumberUtil;
    private final EmailService emailService;

    @Value("${security.email.verification.token.expiry:86400}") // 24 hours in seconds
    private long tokenExpirySeconds;

    /**
     * Register a new healthcare provider
     */
    @Transactional
    public ProviderResponseDTO registerProvider(ProviderRegisterDTO registerDTO, String ipAddress) {
        log.info("Starting provider registration for email: {}", registerDTO.getEmail());

        // Validate input data
        Map<String, Object> validationResult = validateRegistrationData(registerDTO);
        if (!(Boolean) validationResult.get("isValid")) {
            throw new ValidationException("Validation failed", (Map<String, List<String>>) validationResult.get("errors"));
        }

        // Check for existing provider
        checkForDuplicates(registerDTO);

        // Normalize and sanitize input data
        Provider provider = createProviderFromDTO(registerDTO);

        try {
            // Save provider to database
            Provider savedProvider = providerRepository.save(provider);
            log.info("Provider saved successfully with ID: {}", savedProvider.getId());

            // Send verification email asynchronously
            try {
                emailService.sendVerificationEmail(
                    savedProvider.getEmail(),
                    savedProvider.getFirstName(),
                    savedProvider.getEmailVerificationToken()
                );
            } catch (Exception e) {
                log.error("Failed to send verification email for provider {}: {}", savedProvider.getId(), e.getMessage());
                // Don't fail registration if email sending fails
            }

            // Convert to response DTO
            ProviderResponseDTO responseDTO = convertToResponseDTO(savedProvider);
            
            log.info("Provider registration completed successfully for: {}", savedProvider.getEmail());
            return responseDTO;

        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation during provider registration: {}", e.getMessage());
            throw new ConflictException("Provider with this email, phone, or license number already exists");
        } catch (Exception e) {
            log.error("Unexpected error during provider registration: {}", e.getMessage(), e);
            throw new RuntimeException("Registration failed due to internal error");
        }
    }

    /**
     * Verify email using verification token
     */
    @Transactional
    public boolean verifyEmail(String token) {
        log.info("Verifying email with token: {}", token.substring(0, 8) + "...");

        Optional<Provider> providerOpt = providerRepository.findByEmailVerificationToken(token);
        if (providerOpt.isEmpty()) {
            log.warn("Invalid verification token: {}", token.substring(0, 8) + "...");
            return false;
        }

        Provider provider = providerOpt.get();

        // Check if token is expired
        if (provider.isEmailVerificationTokenExpired()) {
            log.warn("Expired verification token for provider: {}", provider.getEmail());
            return false;
        }

        // Mark email as verified
        provider.setEmailVerified(true);
        provider.setEmailVerificationToken(null);
        provider.setEmailVerificationTokenExpiry(null);

        providerRepository.save(provider);

        // Send welcome email
        try {
            emailService.sendWelcomeEmail(provider.getEmail(), provider.getFirstName());
        } catch (Exception e) {
            log.error("Failed to send welcome email for provider {}: {}", provider.getId(), e.getMessage());
        }

        log.info("Email verified successfully for provider: {}", provider.getEmail());
        return true;
    }

    /**
     * Get provider by ID
     */
    public Optional<ProviderResponseDTO> getProviderById(UUID uuid) {
        return providerRepository.findByUuid(uuid)
            .map(this::convertToResponseDTO);
    }

    /**
     * Get provider by email
     */
    public Optional<ProviderResponseDTO> getProviderByEmail(String email) {
        return providerRepository.findByEmail(email.toLowerCase().trim())
            .map(this::convertToResponseDTO);
    }

    /**
     * Get providers by verification status
     */
    public List<ProviderResponseDTO> getProvidersByStatus(VerificationStatus status) {
        return providerRepository.findByVerificationStatus(status)
            .stream()
            .map(this::convertToResponseDTO)
            .toList();
    }

    /**
     * Get all providers with pagination
     */
    public Page<ProviderResponseDTO> getAllProviders(Pageable pageable) {
        log.info("Retrieving all providers with pagination: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        return providerRepository.findAll(pageable)
            .map(this::convertToResponseDTO);
    }

    /**
     * Update provider information
     */
    @Transactional
    public ProviderResponseDTO updateProvider(UUID uuid, ProviderUpdateDTO updateDTO) {
        log.info("Updating provider with UUID: {}", uuid);

        Provider provider = providerRepository.findByUuid(uuid)
            .orElseThrow(() -> new NotFoundException("Provider not found with UUID: " + uuid));

        // Validate update data
        validateUpdateData(updateDTO, provider);

        // Update fields if provided
        updateProviderFields(provider, updateDTO);

        try {
            Provider updatedProvider = providerRepository.save(provider);
            log.info("Provider updated successfully: {}", uuid);
            return convertToResponseDTO(updatedProvider);
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation during provider update: {}", e.getMessage());
            throw new ConflictException("Provider with this email, phone, or license number already exists");
        }
    }

    /**
     * Delete provider (soft delete by marking as inactive)
     */
    @Transactional
    public void deleteProvider(UUID uuid) {
        log.info("Soft deleting provider with UUID: {}", uuid);

        Provider provider = providerRepository.findByUuid(uuid)
            .orElseThrow(() -> new NotFoundException("Provider not found with UUID: " + uuid));

        provider.setIsActive(false);
        providerRepository.save(provider);
        
        log.info("Provider soft deleted successfully: {}", uuid);
    }

    /**
     * Hard delete provider (permanent deletion)
     */
    @Transactional
    public void hardDeleteProvider(UUID uuid) {
        log.info("Hard deleting provider with UUID: {}", uuid);

        Provider provider = providerRepository.findByUuid(uuid)
            .orElseThrow(() -> new NotFoundException("Provider not found with UUID: " + uuid));

        providerRepository.delete(provider);
        log.info("Provider permanently deleted: {}", uuid);
    }

    /**
     * Search providers by various criteria
     */
    public List<ProviderResponseDTO> searchProviders(String searchTerm) {
        log.info("Searching providers with term: {}", searchTerm);
        
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return List.of();
        }

        String normalizedTerm = searchTerm.toLowerCase().trim();
        List<Provider> providers = providerRepository.findAll()
            .stream()
            .filter(p -> p.getFirstName().toLowerCase().contains(normalizedTerm) ||
                        p.getLastName().toLowerCase().contains(normalizedTerm) ||
                        p.getEmail().toLowerCase().contains(normalizedTerm))
            .toList();
        
        return providers.stream()
            .map(this::convertToResponseDTO)
            .toList();
    }

    /**
     * Activate or deactivate provider
     */
    @Transactional
    public ProviderResponseDTO updateProviderStatus(UUID uuid, boolean isActive) {
        log.info("Updating provider status: {} to {}", uuid, isActive);

        Provider provider = providerRepository.findByUuid(uuid)
            .orElseThrow(() -> new NotFoundException("Provider not found with UUID: " + uuid));

        provider.setIsActive(isActive);
        Provider updatedProvider = providerRepository.save(provider);
        
        log.info("Provider status updated successfully: {}", uuid);
        return convertToResponseDTO(updatedProvider);
    }

    /**
     * Update provider verification status
     */
    @Transactional
    public ProviderResponseDTO updateVerificationStatus(UUID uuid, VerificationStatus status) {
        log.info("Updating provider verification status: {} to {}", uuid, status);

        Provider provider = providerRepository.findByUuid(uuid)
            .orElseThrow(() -> new NotFoundException("Provider not found with UUID: " + uuid));

        provider.setVerificationStatus(status);
        Provider updatedProvider = providerRepository.save(provider);
        
        // TODO: Send notification email based on status
        // Note: Email methods need to be implemented in EmailService
        log.info("Provider verification status changed to {} for provider {}", status, uuid);

        log.info("Provider verification status updated successfully: {}", uuid);
        return convertToResponseDTO(updatedProvider);
    }

    private Map<String, Object> validateRegistrationData(ProviderRegisterDTO dto) {
        Map<String, Object> result = new HashMap<>();
        Map<String, List<String>> errors = new HashMap<>();

        // Validate password match
        if (!passwordUtil.passwordsMatch(dto.getPassword(), dto.getConfirmPassword())) {
            errors.put("confirmPassword", List.of("Passwords do not match"));
        }

        // Validate password complexity
        if (!passwordUtil.isValidPassword(dto.getPassword())) {
            errors.put("password", List.of(
                "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character"
            ));
        }

        // Validate phone number
        if (!phoneNumberUtil.isValidPhoneNumber(dto.getPhoneNumber())) {
            errors.put("phoneNumber", List.of("Invalid phone number format"));
        }

        result.put("isValid", errors.isEmpty());
        result.put("errors", errors);
        return result;
    }

    private void checkForDuplicates(ProviderRegisterDTO dto) {
        // Check email uniqueness
        if (providerRepository.existsByEmail(dto.getEmail().toLowerCase().trim())) {
            throw new ConflictException("Email address is already registered");
        }

        // Check phone number uniqueness
        try {
            String normalizedPhone = phoneNumberUtil.normalizePhoneNumber(dto.getPhoneNumber());
            if (providerRepository.existsByPhoneNumber(normalizedPhone)) {
                throw new ConflictException("Phone number is already registered");
            }
        } catch (NumberParseException e) {
            throw new ValidationException("Invalid phone number format");
        }

        // Check license number uniqueness
        String normalizedLicense = dto.getLicenseNumber().toUpperCase().trim();
        if (providerRepository.existsByLicenseNumber(normalizedLicense)) {
            throw new ConflictException("License number is already registered");
        }
    }

    private Provider createProviderFromDTO(ProviderRegisterDTO dto) {
        Provider provider = new Provider();

        // Set basic information
        provider.setFirstName(dto.getFirstName().trim());
        provider.setLastName(dto.getLastName().trim());
        provider.setEmail(dto.getEmail().toLowerCase().trim());
        provider.setSpecialization(dto.getSpecialization());
        provider.setYearsOfExperience(dto.getYearsOfExperience());
        provider.setLicenseNumber(dto.getLicenseNumber().toUpperCase().trim());

        // Normalize phone number
        try {
            String normalizedPhone = phoneNumberUtil.normalizePhoneNumber(dto.getPhoneNumber());
            provider.setPhoneNumber(normalizedPhone);
        } catch (NumberParseException e) {
            throw new ValidationException("Invalid phone number format");
        }

        // Hash password
        provider.setPasswordHash(passwordUtil.hashPassword(dto.getPassword()));

        // Set clinic address
        ClinicAddress address = new ClinicAddress();
        address.setStreet(dto.getClinicAddress().getStreet().trim());
        address.setCity(dto.getClinicAddress().getCity().trim());
        address.setState(dto.getClinicAddress().getState().trim());
        address.setZip(dto.getClinicAddress().getZip().trim());
        provider.setClinicAddress(address);

        // Set default values
        provider.setVerificationStatus(VerificationStatus.PENDING);
        provider.setEmailVerified(false);
        provider.setIsActive(true);

        // Generate email verification token
        String verificationToken = passwordUtil.generateSecureToken();
        provider.setEmailVerificationToken(verificationToken);
        provider.setEmailVerificationTokenExpiry(LocalDateTime.now().plusSeconds(tokenExpirySeconds));

        return provider;
    }

    private ProviderResponseDTO convertToResponseDTO(Provider provider) {
        ProviderResponseDTO dto = new ProviderResponseDTO();
        dto.setId(provider.getId());
        dto.setUuid(provider.getUuid());
        dto.setFirstName(provider.getFirstName());
        dto.setLastName(provider.getLastName());
        dto.setEmail(provider.getEmail());
        dto.setPhoneNumber(provider.getPhoneNumber());
        dto.setSpecialization(provider.getSpecialization());
        dto.setLicenseNumber(provider.getLicenseNumber());
        dto.setYearsOfExperience(provider.getYearsOfExperience());
        dto.setVerificationStatus(provider.getVerificationStatus());
        dto.setEmailVerified(provider.getEmailVerified());
        dto.setIsActive(provider.getIsActive());
        dto.setCreatedAt(provider.getCreatedAt());

        // Convert clinic address
        if (provider.getClinicAddress() != null) {
            ProviderResponseDTO.ClinicAddressResponseDTO addressDTO = new ProviderResponseDTO.ClinicAddressResponseDTO();
            addressDTO.setStreet(provider.getClinicAddress().getStreet());
            addressDTO.setCity(provider.getClinicAddress().getCity());
            addressDTO.setState(provider.getClinicAddress().getState());
            addressDTO.setZip(provider.getClinicAddress().getZip());
            dto.setClinicAddress(addressDTO);
        }

        return dto;
    }

    private void validateUpdateData(ProviderUpdateDTO updateDTO, Provider existingProvider) {
        Map<String, List<String>> errors = new HashMap<>();

        // Validate phone number if provided
        if (updateDTO.getPhoneNumber() != null && !phoneNumberUtil.isValidPhoneNumber(updateDTO.getPhoneNumber())) {
            errors.put("phoneNumber", List.of("Invalid phone number format"));
        }

        // Check for duplicates if email is being updated
        if (updateDTO.getEmail() != null && !updateDTO.getEmail().equals(existingProvider.getEmail())) {
            if (providerRepository.existsByEmail(updateDTO.getEmail().toLowerCase().trim())) {
                errors.put("email", List.of("Email address is already in use"));
            }
        }

        // Check for duplicate phone number if being updated
        if (updateDTO.getPhoneNumber() != null) {
            try {
                String normalizedPhone = phoneNumberUtil.normalizePhoneNumber(updateDTO.getPhoneNumber());
                if (!normalizedPhone.equals(existingProvider.getPhoneNumber()) && 
                    providerRepository.existsByPhoneNumber(normalizedPhone)) {
                    errors.put("phoneNumber", List.of("Phone number is already in use"));
                }
            } catch (NumberParseException e) {
                errors.put("phoneNumber", List.of("Invalid phone number format"));
            }
        }

        // Check for duplicate license number if being updated
        if (updateDTO.getLicenseNumber() != null) {
            String normalizedLicense = updateDTO.getLicenseNumber().toUpperCase().trim();
            if (!normalizedLicense.equals(existingProvider.getLicenseNumber()) && 
                providerRepository.existsByLicenseNumber(normalizedLicense)) {
                errors.put("licenseNumber", List.of("License number is already in use"));
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationException("Validation failed", errors);
        }
    }

    private void updateProviderFields(Provider provider, ProviderUpdateDTO updateDTO) {
        if (updateDTO.getFirstName() != null) {
            provider.setFirstName(updateDTO.getFirstName().trim());
        }
        if (updateDTO.getLastName() != null) {
            provider.setLastName(updateDTO.getLastName().trim());
        }
        if (updateDTO.getEmail() != null) {
            provider.setEmail(updateDTO.getEmail().toLowerCase().trim());
        }
        if (updateDTO.getPhoneNumber() != null) {
            try {
                String normalizedPhone = phoneNumberUtil.normalizePhoneNumber(updateDTO.getPhoneNumber());
                provider.setPhoneNumber(normalizedPhone);
            } catch (NumberParseException e) {
                throw new ValidationException("Invalid phone number format");
            }
        }
        if (updateDTO.getSpecialization() != null) {
            provider.setSpecialization(updateDTO.getSpecialization());
        }
        if (updateDTO.getLicenseNumber() != null) {
            provider.setLicenseNumber(updateDTO.getLicenseNumber().toUpperCase().trim());
        }
        if (updateDTO.getYearsOfExperience() != null) {
            provider.setYearsOfExperience(updateDTO.getYearsOfExperience());
        }
        if (updateDTO.getClinicAddress() != null) {
            ClinicAddress address = provider.getClinicAddress();
            if (address == null) {
                address = new ClinicAddress();
                provider.setClinicAddress(address);
            }
            if (updateDTO.getClinicAddress().getStreet() != null) {
                address.setStreet(updateDTO.getClinicAddress().getStreet().trim());
            }
            if (updateDTO.getClinicAddress().getCity() != null) {
                address.setCity(updateDTO.getClinicAddress().getCity().trim());
            }
            if (updateDTO.getClinicAddress().getState() != null) {
                address.setState(updateDTO.getClinicAddress().getState().trim());
            }
            if (updateDTO.getClinicAddress().getZip() != null) {
                address.setZip(updateDTO.getClinicAddress().getZip().trim());
            }
        }
        if (updateDTO.getVerificationStatus() != null) {
            provider.setVerificationStatus(updateDTO.getVerificationStatus());
        }
        if (updateDTO.getLicenseDocumentUrl() != null) {
            provider.setLicenseDocumentUrl(updateDTO.getLicenseDocumentUrl());
        }
        if (updateDTO.getIsActive() != null) {
            provider.setIsActive(updateDTO.getIsActive());
        }
    }

    // Custom exception classes
    public static class ValidationException extends RuntimeException {
        private final Map<String, List<String>> errors;

        public ValidationException(String message) {
            super(message);
            this.errors = new HashMap<>();
        }

        public ValidationException(String message, Map<String, List<String>> errors) {
            super(message);
            this.errors = errors;
        }

        public Map<String, List<String>> getErrors() {
            return errors;
        }
    }

    public static class ConflictException extends RuntimeException {
        public ConflictException(String message) {
            super(message);
        }
    }

    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String message) {
            super(message);
        }
    }
} 