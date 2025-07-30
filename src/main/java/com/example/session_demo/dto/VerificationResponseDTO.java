package com.example.session_demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationResponseDTO {

    @JsonProperty("patient_id")
    private UUID patientId;

    @JsonProperty("verification_type")
    private String verificationType;

    @JsonProperty("verification_status")
    private String verificationStatus;

    @JsonProperty("attempts_remaining")
    private Integer attemptsRemaining;

    @JsonProperty("expires_at")
    private LocalDateTime expiresAt;

    @JsonProperty("next_attempt_allowed_at")
    private LocalDateTime nextAttemptAllowedAt;

    @JsonProperty("account_fully_verified")
    private Boolean accountFullyVerified;

    private String message;

    // Factory methods for different scenarios
    public static VerificationResponseDTO sent(UUID patientId, String verificationType, 
                                             int attemptsRemaining, LocalDateTime expiresAt) {
        return VerificationResponseDTO.builder()
                .patientId(patientId)
                .verificationType(verificationType)
                .verificationStatus("SENT")
                .attemptsRemaining(attemptsRemaining)
                .expiresAt(expiresAt)
                .accountFullyVerified(false)
                .message("Verification " + verificationType + " sent successfully")
                .build();
    }

    public static VerificationResponseDTO verified(UUID patientId, String verificationType, 
                                                 boolean accountFullyVerified) {
        return VerificationResponseDTO.builder()
                .patientId(patientId)
                .verificationType(verificationType)
                .verificationStatus("VERIFIED")
                .accountFullyVerified(accountFullyVerified)
                .message(verificationType + " verification completed successfully")
                .build();
    }

    public static VerificationResponseDTO failed(UUID patientId, String verificationType, 
                                               int attemptsRemaining, String reason) {
        return VerificationResponseDTO.builder()
                .patientId(patientId)
                .verificationType(verificationType)
                .verificationStatus("FAILED")
                .attemptsRemaining(attemptsRemaining)
                .accountFullyVerified(false)
                .message("Verification failed: " + reason)
                .build();
    }

    public static VerificationResponseDTO expired(UUID patientId, String verificationType) {
        return VerificationResponseDTO.builder()
                .patientId(patientId)
                .verificationType(verificationType)
                .verificationStatus("EXPIRED")
                .attemptsRemaining(0)
                .accountFullyVerified(false)
                .message("Verification token has expired. Please request a new one.")
                .build();
    }

    public static VerificationResponseDTO rateLimited(UUID patientId, String verificationType, 
                                                    LocalDateTime nextAttemptAllowedAt) {
        return VerificationResponseDTO.builder()
                .patientId(patientId)
                .verificationType(verificationType)
                .verificationStatus("RATE_LIMITED")
                .nextAttemptAllowedAt(nextAttemptAllowedAt)
                .accountFullyVerified(false)
                .message("Too many attempts. Please try again later.")
                .build();
    }
} 