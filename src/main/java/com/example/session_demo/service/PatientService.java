package com.example.session_demo.service;

import com.example.session_demo.dto.PatientRegistrationRequestDTO;
import com.example.session_demo.dto.PatientRegistrationResponseDTO;
import com.example.session_demo.dto.VerificationRequestDTO;
import com.example.session_demo.dto.VerificationResponseDTO;
import com.example.session_demo.entity.Patient;

import java.util.Optional;
import java.util.UUID;

public interface PatientService {
    
    /**
     * Register a new patient with comprehensive validation and HIPAA compliance
     */
    PatientRegistrationResponseDTO registerPatient(PatientRegistrationRequestDTO request, String ipAddress, String userAgent);
    
    /**
     * Verify patient email using verification token
     */
    VerificationResponseDTO verifyEmail(String token, String ipAddress);
    
    /**
     * Verify patient phone using OTP code
     */
    VerificationResponseDTO verifyPhone(VerificationRequestDTO request, String ipAddress);
    
    /**
     * Resend verification (email or SMS)
     */
    VerificationResponseDTO resendVerification(VerificationRequestDTO request, String ipAddress);
    
    /**
     * Find patient by UUID
     */
    Optional<Patient> findByUuid(UUID uuid);
    
    /**
     * Find patient by email
     */
    Optional<Patient> findByEmail(String email);
    
    /**
     * Check if email already exists
     */
    boolean existsByEmail(String email);
    
    /**
     * Check if phone number already exists
     */
    boolean existsByPhoneNumber(String phoneNumber);
} 