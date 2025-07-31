package com.example.session_demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientRegistrationResponseDTO {

    @JsonProperty("patient_id")
    private UUID patientId;

    private String email;

    @JsonProperty("phone_number")
    private String phoneNumber;

    @JsonProperty("email_verified")
    private Boolean emailVerified;

    @JsonProperty("phone_verified")
    private Boolean phoneVerified;

    @JsonProperty("verification_methods")
    private List<String> verificationMethods;

    @JsonProperty("next_steps")
    private List<String> nextSteps;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("account_status")
    private String accountStatus;

    @JsonProperty("requires_age_verification")
    private Boolean requiresAgeVerification;

    @JsonProperty("privacy_consent_given")
    private Boolean privacyConsentGiven;

    @JsonProperty("terms_accepted")
    private Boolean termsAccepted;

    // Helper method to create a successful response
    public static PatientRegistrationResponseDTO success(
            UUID patientId, 
            String email, 
            String phoneNumber, 
            boolean isMinor,
            LocalDateTime createdAt) {
        
        List<String> verificationMethods = List.of("email", "sms");
        List<String> nextSteps;
        
        if (isMinor) {
            nextSteps = List.of(
                "Check your email for verification link",
                "Check your phone for SMS verification code",
                "Parental consent required for account activation"
            );
        } else {
            nextSteps = List.of(
                "Check your email for verification link", 
                "Check your phone for SMS verification code"
            );
        }
        
        return PatientRegistrationResponseDTO.builder()
                .patientId(patientId)
                .email(email)
                .phoneNumber(phoneNumber)
                .emailVerified(false)
                .phoneVerified(false)
                .verificationMethods(verificationMethods)
                .nextSteps(nextSteps)
                .createdAt(createdAt)
                .accountStatus(isMinor ? "PENDING_PARENTAL_CONSENT" : "PENDING_VERIFICATION")
                .requiresAgeVerification(isMinor)
                .privacyConsentGiven(true)
                .termsAccepted(true)
                .build();
    }
} 