package com.example.session_demo.dto;

import com.example.session_demo.enums.ProviderSpecialization;
import com.example.session_demo.enums.VerificationStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProviderUpdateDTO {

    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    @Email(message = "Email should be valid")
    private String email;

    private String phoneNumber;

    private ProviderSpecialization specialization;

    @Pattern(regexp = "^[A-Za-z0-9]{6,20}$", message = "License number must be alphanumeric and between 6-20 characters")
    private String licenseNumber;

    @Min(value = 0, message = "Years of experience cannot be negative")
    @Max(value = 50, message = "Years of experience cannot exceed 50")
    private Integer yearsOfExperience;

    @Valid
    private ClinicAddressDTO clinicAddress;

    private VerificationStatus verificationStatus;

    private String licenseDocumentUrl;

    private Boolean isActive;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClinicAddressDTO {
        @Size(max = 200, message = "Street address must not exceed 200 characters")
        private String street;

        @Size(max = 100, message = "City must not exceed 100 characters")
        private String city;

        @Size(max = 50, message = "State must not exceed 50 characters")
        private String state;

        @Pattern(regexp = "^\\d{5}(-\\d{4})?$", message = "ZIP code must be in format 12345 or 12345-6789")
        private String zip;
    }
} 