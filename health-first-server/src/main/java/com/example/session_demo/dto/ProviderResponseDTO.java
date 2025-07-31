package com.example.session_demo.dto;

import com.example.session_demo.enums.ProviderSpecialization;
import com.example.session_demo.enums.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProviderResponseDTO {
    private Long id;
    private UUID uuid;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private ProviderSpecialization specialization;
    private String licenseNumber;
    private Integer yearsOfExperience;
    private ClinicAddressResponseDTO clinicAddress;
    private VerificationStatus verificationStatus;
    private Boolean emailVerified;
    private Boolean isActive;
    private LocalDateTime createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClinicAddressResponseDTO {
        private String street;
        private String city;
        private String state;
        private String zip;
        
        public String getFullAddress() {
            return String.format("%s, %s, %s %s", street, city, state, zip);
        }
    }
} 