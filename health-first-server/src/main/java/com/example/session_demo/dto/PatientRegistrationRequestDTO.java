package com.example.session_demo.dto;

import com.example.session_demo.enums.Gender;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatientRegistrationRequestDTO {

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    @JsonProperty("first_name")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    @JsonProperty("last_name")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Phone number must be in valid international format")
    @JsonProperty("phone_number")
    private String phoneNumber;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&].*$", 
             message = "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character")
    private String password;

    @NotBlank(message = "Password confirmation is required")
    @JsonProperty("confirm_password")
    private String confirmPassword;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    @JsonProperty("date_of_birth")
    private LocalDate dateOfBirth;

    @NotNull(message = "Gender is required")
    private Gender gender;

    @Valid
    @NotNull(message = "Address is required")
    private AddressDTO address;

    @Valid
    @JsonProperty("emergency_contact")
    private EmergencyContactDTO emergencyContact;

    @JsonProperty("medical_history")
    private List<@NotBlank @Size(max = 200) String> medicalHistory;

    private List<@NotBlank @Size(max = 100) String> allergies;

    @JsonProperty("current_medications")
    private List<@NotBlank @Size(max = 200) String> currentMedications;

    @Valid
    @JsonProperty("insurance_info")
    private InsuranceInfoDTO insuranceInfo;

    @Size(max = 10, message = "Preferred language code must not exceed 10 characters")
    @JsonProperty("preferred_language")
    private String preferredLanguage = "en";

    @Valid
    @JsonProperty("communication_preferences")
    private CommunicationPreferencesDTO communicationPreferences = new CommunicationPreferencesDTO();

    @NotNull(message = "Privacy consent is required")
    @AssertTrue(message = "Privacy consent must be accepted")
    @JsonProperty("privacy_consent")
    private Boolean privacyConsent;

    @NotNull(message = "Terms acceptance is required")
    @AssertTrue(message = "Terms of service must be accepted")
    @JsonProperty("terms_accepted")
    private Boolean termsAccepted;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressDTO {
        @NotBlank(message = "Street address is required")
        @Size(max = 200, message = "Street address must not exceed 200 characters")
        private String street;

        @NotBlank(message = "City is required")
        @Size(max = 100, message = "City must not exceed 100 characters")
        private String city;

        @NotBlank(message = "State is required")
        @Size(max = 50, message = "State must not exceed 50 characters")
        private String state;

        @NotBlank(message = "ZIP code is required")
        @Pattern(regexp = "^\\d{5}(-\\d{4})?$", message = "ZIP code must be in format 12345 or 12345-6789")
        private String zip;

        @Size(max = 50, message = "Country must not exceed 50 characters")
        private String country = "US";
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmergencyContactDTO {
        @Size(max = 100, message = "Emergency contact name must not exceed 100 characters")
        private String name;

        @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Emergency contact phone must be a valid international format")
        private String phone;

        @Size(max = 50, message = "Emergency contact relationship must not exceed 50 characters")
        private String relationship;

        @Email(message = "Emergency contact email must be valid")
        @Size(max = 255, message = "Emergency contact email must not exceed 255 characters")
        private String email;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InsuranceInfoDTO {
        @Size(max = 255, message = "Insurance provider name must not exceed 255 characters")
        private String provider;

        @Size(max = 100, message = "Policy number must not exceed 100 characters")
        @JsonProperty("policy_number")
        private String policyNumber;

        @Size(max = 100, message = "Group number must not exceed 100 characters")
        @JsonProperty("group_number")
        private String groupNumber;

        @Size(max = 100, message = "Member ID must not exceed 100 characters")
        @JsonProperty("member_id")
        private String memberId;

        @JsonProperty("effective_date")
        private LocalDate effectiveDate;

        @JsonProperty("expiry_date")
        private LocalDate expiryDate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommunicationPreferencesDTO {
        @JsonProperty("email_notifications")
        private Boolean emailNotifications = true;

        @JsonProperty("sms_notifications")
        private Boolean smsNotifications = false;

        @JsonProperty("marketing_emails")
        private Boolean marketingEmails = false;

        @JsonProperty("appointment_reminders")
        private Boolean appointmentReminders = true;
    }

    // Custom validation methods
    public boolean isPasswordMatch() {
        return password != null && password.equals(confirmPassword);
    }

    public boolean isValidAge(int minimumAge) {
        if (dateOfBirth == null) {
            return false;
        }
        LocalDate minimumBirthDate = LocalDate.now().minusYears(minimumAge);
        return dateOfBirth.isBefore(minimumBirthDate) || dateOfBirth.isEqual(minimumBirthDate);
    }

    public int getCalculatedAge() {
        if (dateOfBirth == null) {
            return 0;
        }
        return java.time.Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    // Data sanitization methods
    public void sanitizeData() {
        if (firstName != null) firstName = firstName.trim();
        if (lastName != null) lastName = lastName.trim();
        if (email != null) email = email.toLowerCase().trim();
        if (phoneNumber != null) phoneNumber = phoneNumber.trim();
        if (preferredLanguage != null) preferredLanguage = preferredLanguage.toLowerCase().trim();
        
        if (address != null) {
            if (address.street != null) address.street = address.street.trim();
            if (address.city != null) address.city = address.city.trim();
            if (address.state != null) address.state = address.state.trim().toUpperCase();
            if (address.zip != null) address.zip = address.zip.trim();
            if (address.country != null) address.country = address.country.trim().toUpperCase();
        }
        
        if (emergencyContact != null) {
            if (emergencyContact.name != null) emergencyContact.name = emergencyContact.name.trim();
            if (emergencyContact.phone != null) emergencyContact.phone = emergencyContact.phone.trim();
            if (emergencyContact.relationship != null) emergencyContact.relationship = emergencyContact.relationship.trim().toLowerCase();
            if (emergencyContact.email != null) emergencyContact.email = emergencyContact.email.toLowerCase().trim();
        }
        
        if (insuranceInfo != null) {
            if (insuranceInfo.provider != null) insuranceInfo.provider = insuranceInfo.provider.trim();
            if (insuranceInfo.policyNumber != null) insuranceInfo.policyNumber = insuranceInfo.policyNumber.trim();
            if (insuranceInfo.groupNumber != null) insuranceInfo.groupNumber = insuranceInfo.groupNumber.trim();
            if (insuranceInfo.memberId != null) insuranceInfo.memberId = insuranceInfo.memberId.trim();
        }
    }
} 