package com.example.session_demo.service;

import com.example.session_demo.dto.PatientRegistrationRequestDTO;
import com.example.session_demo.dto.PatientRegistrationResponseDTO;
import com.example.session_demo.dto.VerificationRequestDTO;
import com.example.session_demo.dto.VerificationResponseDTO;
import com.example.session_demo.entity.*;
import com.example.session_demo.repository.PatientAuditLogRepository;
import com.example.session_demo.repository.PatientRepository;
import com.example.session_demo.repository.VerificationTokenRepository;
import com.example.session_demo.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PatientAuditLogRepository auditLogRepository;
    private final EncryptionService encryptionService;
    private final PasswordUtil passwordUtil;
    private final EmailService emailService;
    private final SmsService smsService;

    private static final int MINIMUM_AGE = 13; // COPPA compliance
    private static final SecureRandom secureRandom = new SecureRandom();

    @Override
    public PatientRegistrationResponseDTO registerPatient(PatientRegistrationRequestDTO request, String ipAddress, String userAgent) {
        log.info("Starting patient registration for email: {}", request.getEmail());

        try {
            // Validate age requirement
            if (!request.isValidAge(MINIMUM_AGE)) {
                logAuditEvent(null, PatientAuditLog.ActionType.PATIENT_REGISTRATION, 
                             false, "Age below minimum requirement", ipAddress, userAgent);
                throw new IllegalArgumentException("Patient must be at least " + MINIMUM_AGE + " years old");
            }

            // Check for duplicate email/phone
            if (existsByEmail(request.getEmail())) {
                logAuditEvent(null, PatientAuditLog.ActionType.PATIENT_REGISTRATION, 
                             false, "Duplicate email address", ipAddress, userAgent);
                throw new IllegalArgumentException("Email address already exists");
            }

            if (existsByPhoneNumber(request.getPhoneNumber())) {
                logAuditEvent(null, PatientAuditLog.ActionType.PATIENT_REGISTRATION, 
                             false, "Duplicate phone number", ipAddress, userAgent);
                throw new IllegalArgumentException("Phone number already exists");
            }

            // Create patient entity
            Patient patient = createPatientFromRequest(request);
            
            // Save patient
            patient = patientRepository.save(patient);
            log.info("Patient created with UUID: {}", patient.getUuid());

            // Log successful registration
            logAuditEvent(patient, PatientAuditLog.ActionType.PATIENT_REGISTRATION, 
                         true, "Patient registration successful", ipAddress, userAgent);

            // Send verification emails/SMS
            sendVerificationTokens(patient, ipAddress, userAgent);

            // Build response
            return PatientRegistrationResponseDTO.success(
                patient.getUuid(),
                patient.getEmail(),
                patient.getPhoneNumber(),
                patient.isMinor(),
                patient.getCreatedAt()
            );

        } catch (Exception e) {
            log.error("Patient registration failed for email: {}", request.getEmail(), e);
            logAuditEvent(null, PatientAuditLog.ActionType.PATIENT_REGISTRATION, 
                         false, "Registration failed: " + e.getMessage(), ipAddress, userAgent);
            throw e;
        }
    }

    @Override
    public VerificationResponseDTO verifyEmail(String token, String ipAddress) {
        log.info("Email verification attempt from IP: {}", ipAddress);

        try {
            // Find verification token
            String tokenHash = encryptionService.generateHash(token);
            Optional<VerificationToken> tokenOpt = verificationTokenRepository.findByTokenHash(tokenHash);

            if (tokenOpt.isEmpty()) {
                logAuditEvent(null, PatientAuditLog.ActionType.EMAIL_VERIFICATION_COMPLETED, 
                             false, "Invalid verification token", ipAddress, null);
                throw new IllegalArgumentException("Invalid verification token");
            }

            VerificationToken verificationToken = tokenOpt.get();
            Patient patient = verificationToken.getPatient();

            // Validate token
            if (!verificationToken.canAttempt()) {
                logAuditEvent(patient, PatientAuditLog.ActionType.EMAIL_VERIFICATION_COMPLETED, 
                             false, "Token expired or max attempts exceeded", ipAddress, null);
                throw new IllegalArgumentException("Verification token is expired or has exceeded maximum attempts");
            }

            // Mark email as verified
            patient.setEmailVerified(true);
            verificationToken.markAsUsed();

            patientRepository.save(patient);
            verificationTokenRepository.save(verificationToken);

            // Log successful verification
            logAuditEvent(patient, PatientAuditLog.ActionType.EMAIL_VERIFICATION_COMPLETED, 
                         true, "Email verification successful", ipAddress, null);

            log.info("Email verified successfully for patient: {}", patient.getUuid());

            return VerificationResponseDTO.verified(
                patient.getUuid(), 
                "email", 
                patient.isFullyVerified()
            );

        } catch (Exception e) {
            log.error("Email verification failed", e);
            throw e;
        }
    }

    @Override
    public VerificationResponseDTO verifyPhone(VerificationRequestDTO request, String ipAddress) {
        log.info("Phone verification attempt for patient: {}", request.getPatientId());

        try {
            // Find patient
            Optional<Patient> patientOpt = findByUuid(request.getPatientId());
            if (patientOpt.isEmpty()) {
                logAuditEvent(null, PatientAuditLog.ActionType.PHONE_VERIFICATION_COMPLETED, 
                             false, "Patient not found", ipAddress, null);
                throw new IllegalArgumentException("Patient not found");
            }

            Patient patient = patientOpt.get();

            // Find active phone verification token
            Optional<VerificationToken> tokenOpt = verificationTokenRepository.findActiveTokenByPatientAndType(
                patient, VerificationToken.TokenType.PHONE_VERIFICATION, LocalDateTime.now()
            );

            if (tokenOpt.isEmpty()) {
                logAuditEvent(patient, PatientAuditLog.ActionType.PHONE_VERIFICATION_COMPLETED, 
                             false, "No active verification token found", ipAddress, null);
                throw new IllegalArgumentException("No active verification token found");
            }

            VerificationToken verificationToken = tokenOpt.get();

            // Increment attempts
            verificationToken.incrementAttempts();
            verificationTokenRepository.save(verificationToken);

            // Validate OTP code
            String decryptedOtp = encryptionService.decrypt(verificationToken.getTokenValueEncrypted());
            if (!request.getVerificationCode().equals(decryptedOtp)) {
                logAuditEvent(patient, PatientAuditLog.ActionType.PHONE_VERIFICATION_COMPLETED, 
                             false, "Invalid OTP code", ipAddress, null);
                
                return VerificationResponseDTO.failed(
                    patient.getUuid(), 
                    "phone", 
                    verificationToken.getRemainingAttempts(),
                    "Invalid verification code"
                );
            }

            // Mark phone as verified
            patient.setPhoneVerified(true);
            verificationToken.markAsUsed();

            patientRepository.save(patient);
            verificationTokenRepository.save(verificationToken);

            // Log successful verification
            logAuditEvent(patient, PatientAuditLog.ActionType.PHONE_VERIFICATION_COMPLETED, 
                         true, "Phone verification successful", ipAddress, null);

            log.info("Phone verified successfully for patient: {}", patient.getUuid());

            return VerificationResponseDTO.verified(
                patient.getUuid(), 
                "phone", 
                patient.isFullyVerified()
            );

        } catch (Exception e) {
            log.error("Phone verification failed", e);
            throw e;
        }
    }

    @Override
    public VerificationResponseDTO resendVerification(VerificationRequestDTO request, String ipAddress) {
        log.info("Resend verification request for patient: {}, type: {}", 
                request.getPatientId(), request.getVerificationType());

        try {
            // Find patient
            Optional<Patient> patientOpt = findByUuid(request.getPatientId());
            if (patientOpt.isEmpty()) {
                throw new IllegalArgumentException("Patient not found");
            }

            Patient patient = patientOpt.get();

            // Check rate limiting here (implementation depends on your rate limiting service)
            
            if (request.isEmailVerification()) {
                return resendEmailVerification(patient, ipAddress);
            } else if (request.isPhoneVerification()) {
                return resendPhoneVerification(patient, ipAddress);
            } else {
                throw new IllegalArgumentException("Invalid verification type");
            }

        } catch (Exception e) {
            log.error("Resend verification failed", e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Patient> findByUuid(UUID uuid) {
        return patientRepository.findByUuid(uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Patient> findByEmail(String email) {
        return patientRepository.findByEmail(email.toLowerCase().trim());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return patientRepository.existsByEmail(email.toLowerCase().trim());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByPhoneNumber(String phoneNumber) {
        return patientRepository.existsByPhoneNumber(phoneNumber.trim());
    }

    // Private helper methods

    private Patient createPatientFromRequest(PatientRegistrationRequestDTO request) {
        Patient patient = new Patient();
        
        // Basic information
        patient.setFirstName(request.getFirstName());
        patient.setLastName(request.getLastName());
        patient.setEmail(request.getEmail());
        patient.setPhoneNumber(request.getPhoneNumber());
        patient.setDateOfBirth(request.getDateOfBirth());
        patient.setGender(request.getGender());
        patient.setPreferredLanguage(request.getPreferredLanguage());

        // Hash password
        patient.setPasswordHash(passwordUtil.hashPassword(request.getPassword()));
        patient.setPasswordChangedAt(LocalDateTime.now());

        // Address
        if (request.getAddress() != null) {
            PatientAddress address = new PatientAddress();
            address.setStreet(request.getAddress().getStreet());
            address.setCity(request.getAddress().getCity());
            address.setState(request.getAddress().getState());
            address.setZip(request.getAddress().getZip());
            address.setCountry(request.getAddress().getCountry());
            patient.setAddress(address);
        }

        // Emergency contact
        if (request.getEmergencyContact() != null) {
            EmergencyContact emergencyContact = new EmergencyContact();
            emergencyContact.setName(request.getEmergencyContact().getName());
            emergencyContact.setPhone(request.getEmergencyContact().getPhone());
            emergencyContact.setRelationship(request.getEmergencyContact().getRelationship());
            emergencyContact.setEmail(request.getEmergencyContact().getEmail());
            patient.setEmergencyContact(emergencyContact);
        }

        // Encrypt and store medical data
        if (request.getMedicalHistory() != null && !request.getMedicalHistory().isEmpty()) {
            patient.setMedicalHistoryEncrypted(encryptionService.encryptList(request.getMedicalHistory()));
        }

        if (request.getAllergies() != null && !request.getAllergies().isEmpty()) {
            patient.setAllergiesEncrypted(encryptionService.encryptList(request.getAllergies()));
        }

        if (request.getCurrentMedications() != null && !request.getCurrentMedications().isEmpty()) {
            patient.setCurrentMedicationsEncrypted(encryptionService.encryptList(request.getCurrentMedications()));
        }

        // Insurance information
        if (request.getInsuranceInfo() != null) {
            InsuranceInfo insuranceInfo = new InsuranceInfo();
            insuranceInfo.setProvider(request.getInsuranceInfo().getProvider());
            insuranceInfo.setGroupNumber(request.getInsuranceInfo().getGroupNumber());
            insuranceInfo.setEffectiveDate(request.getInsuranceInfo().getEffectiveDate());
            insuranceInfo.setExpiryDate(request.getInsuranceInfo().getExpiryDate());

            // Encrypt sensitive insurance data
            if (request.getInsuranceInfo().getPolicyNumber() != null) {
                insuranceInfo.setPolicyNumberEncrypted(
                    encryptionService.encrypt(request.getInsuranceInfo().getPolicyNumber())
                );
            }
            if (request.getInsuranceInfo().getMemberId() != null) {
                insuranceInfo.setMemberIdEncrypted(
                    encryptionService.encrypt(request.getInsuranceInfo().getMemberId())
                );
            }
            patient.setInsuranceInfo(insuranceInfo);
        }

        // Communication preferences
        if (request.getCommunicationPreferences() != null) {
            CommunicationPreferences commPrefs = new CommunicationPreferences();
            commPrefs.setEmailNotifications(request.getCommunicationPreferences().getEmailNotifications());
            commPrefs.setSmsNotifications(request.getCommunicationPreferences().getSmsNotifications());
            commPrefs.setMarketingEmails(request.getCommunicationPreferences().getMarketingEmails());
            commPrefs.setAppointmentReminders(request.getCommunicationPreferences().getAppointmentReminders());
            patient.setCommunicationPreferences(commPrefs);
        }

        // Consent information
        patient.setPrivacyConsent(request.getPrivacyConsent());
        patient.setTermsAccepted(request.getTermsAccepted());
        patient.setConsentDate(LocalDateTime.now());

        return patient;
    }

    private void sendVerificationTokens(Patient patient, String ipAddress, String userAgent) {
        // Send email verification
        sendEmailVerification(patient, ipAddress, userAgent);
        
        // Send SMS verification
        sendPhoneVerification(patient, ipAddress, userAgent);
    }

    private void sendEmailVerification(Patient patient, String ipAddress, String userAgent) {
        try {
            // Generate verification token
            String token = generateSecureToken();
            String tokenHash = encryptionService.generateHash(token);

            // Create verification token entity
            VerificationToken verificationToken = new VerificationToken();
            verificationToken.setPatient(patient);
            verificationToken.setTokenType(VerificationToken.TokenType.EMAIL_VERIFICATION);
            verificationToken.setTokenHash(tokenHash);
            verificationToken.setExpiresAt(VerificationToken.TokenType.EMAIL_VERIFICATION.getDefaultExpiryTime());
            verificationToken.setIpAddress(ipAddress);
            verificationToken.setUserAgent(userAgent);

            verificationTokenRepository.save(verificationToken);

            // Send email
            log.info("🔄 Attempting to send patient verification email to: {}", patient.getEmail());
            emailService.sendPatientVerificationEmail(patient.getEmail(), patient.getFirstName(), token);
            log.info("✅ Patient verification email call completed for: {}", patient.getEmail());

            // Log email sent
            logAuditEvent(patient, PatientAuditLog.ActionType.EMAIL_VERIFICATION_SENT, 
                         true, "Verification email sent", ipAddress, userAgent);

            log.info("Email verification sent to: {}", patient.getEmail());

        } catch (Exception e) {
            log.error("Failed to send email verification", e);
            logAuditEvent(patient, PatientAuditLog.ActionType.EMAIL_VERIFICATION_SENT, 
                         false, "Failed to send verification email: " + e.getMessage(), ipAddress, userAgent);
        }
    }

    private void sendPhoneVerification(Patient patient, String ipAddress, String userAgent) {
        try {
            // Generate 6-digit OTP
            String otp = generateOtpCode();
            String encryptedOtp = encryptionService.encrypt(otp);

            // Create verification token entity
            VerificationToken verificationToken = new VerificationToken();
            verificationToken.setPatient(patient);
            verificationToken.setTokenType(VerificationToken.TokenType.PHONE_VERIFICATION);
            verificationToken.setTokenHash(encryptionService.generateHash(otp)); // For lookup
            verificationToken.setTokenValueEncrypted(encryptedOtp);
            verificationToken.setExpiresAt(VerificationToken.TokenType.PHONE_VERIFICATION.getDefaultExpiryTime());
            verificationToken.setIpAddress(ipAddress);
            verificationToken.setUserAgent(userAgent);

            verificationTokenRepository.save(verificationToken);

            // Send SMS verification
            smsService.sendVerificationCode(patient.getPhoneNumber(), otp);

            // Log SMS sent
            logAuditEvent(patient, PatientAuditLog.ActionType.PHONE_VERIFICATION_SENT, 
                         true, "Verification SMS sent", ipAddress, userAgent);

            log.info("SMS verification sent to: {}", patient.getPhoneNumber());

        } catch (Exception e) {
            log.error("Failed to send SMS verification", e);
            logAuditEvent(patient, PatientAuditLog.ActionType.PHONE_VERIFICATION_SENT, 
                         false, "Failed to send verification SMS: " + e.getMessage(), ipAddress, userAgent);
        }
    }

    private VerificationResponseDTO resendEmailVerification(Patient patient, String ipAddress) {
        // Invalidate existing email tokens
        verificationTokenRepository.markAllTokensAsUsedForPatientAndType(
            patient, VerificationToken.TokenType.EMAIL_VERIFICATION
        );

        // Send new verification
        sendEmailVerification(patient, ipAddress, null);

        return VerificationResponseDTO.sent(
            patient.getUuid(), 
            "email", 
            3, // Default max attempts
            VerificationToken.TokenType.EMAIL_VERIFICATION.getDefaultExpiryTime()
        );
    }

    private VerificationResponseDTO resendPhoneVerification(Patient patient, String ipAddress) {
        // Invalidate existing phone tokens
        verificationTokenRepository.markAllTokensAsUsedForPatientAndType(
            patient, VerificationToken.TokenType.PHONE_VERIFICATION
        );

        // Send new verification
        sendPhoneVerification(patient, ipAddress, null);

        return VerificationResponseDTO.sent(
            patient.getUuid(), 
            "phone", 
            3, // Default max attempts
            VerificationToken.TokenType.PHONE_VERIFICATION.getDefaultExpiryTime()
        );
    }

    private String generateSecureToken() {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private String generateOtpCode() {
        return String.format("%06d", secureRandom.nextInt(1000000));
    }

    private void logAuditEvent(Patient patient, PatientAuditLog.ActionType actionType, 
                              boolean success, String details, String ipAddress, String userAgent) {
        try {
            PatientAuditLog auditLog = new PatientAuditLog();
            auditLog.setPatient(patient);
            auditLog.setActionType(actionType);
            auditLog.setSuccess(success);
            auditLog.setFailureReason(success ? null : details);
            auditLog.setIpAddress(ipAddress);
            auditLog.setUserAgent(userAgent);
            auditLog.setActorType("PATIENT");
            auditLog.setActorId(patient != null ? patient.getUuid().toString() : null);

            // Encrypt sensitive details
            if (details != null) {
                auditLog.setActionDetailsEncrypted(encryptionService.encrypt(details));
            }

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to log audit event", e);
        }
    }
} 