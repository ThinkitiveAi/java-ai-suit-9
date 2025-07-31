package com.example.session_demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerificationRequestDTO {

    @JsonProperty("patient_id")
    private UUID patientId;

    @NotBlank(message = "Verification type is required")
    @Pattern(regexp = "^(email|phone)$", message = "Verification type must be 'email' or 'phone'")
    @JsonProperty("verification_type")
    private String verificationType;

    // For email verification
    @Email(message = "Email must be valid")
    private String email;

    // For phone verification
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Phone number must be in valid international format")
    @JsonProperty("phone_number")
    private String phoneNumber;

    // For verification code submission
    @JsonProperty("verification_code")
    private String verificationCode;

    // For email verification token
    @JsonProperty("verification_token")
    private String verificationToken;

    public boolean isEmailVerification() {
        return "email".equalsIgnoreCase(verificationType);
    }

    public boolean isPhoneVerification() {
        return "phone".equalsIgnoreCase(verificationType);
    }
} 