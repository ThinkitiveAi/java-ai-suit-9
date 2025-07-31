package com.example.session_demo.entity;

import com.example.session_demo.enums.Gender;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "patients", indexes = {
    @Index(name = "idx_patient_email", columnList = "email"),
    @Index(name = "idx_patient_phone", columnList = "phone_number"),
    @Index(name = "idx_patient_active", columnList = "is_active"),
    @Index(name = "idx_patient_verified", columnList = "email_verified, phone_verified"),
    @Index(name = "idx_patient_created", columnList = "created_at"),
    @Index(name = "idx_patient_dob", columnList = "date_of_birth")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "UUID", unique = true, nullable = false)
    private UUID uuid;

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    @Column(name = "first_name", nullable = false)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    @Column(name = "last_name", nullable = false)
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Phone number must be in valid international format")
    @Column(name = "phone_number", nullable = false, unique = true)
    private String phoneNumber;

    @JsonIgnore
    @NotBlank(message = "Password is required")
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @NotNull(message = "Gender is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false)
    private Gender gender;

    @Embedded
    @NotNull(message = "Address is required")
    private PatientAddress address;

    @Embedded
    private EmergencyContact emergencyContact;

    // Medical history stored as encrypted JSON array
    @Lob
    @Column(name = "medical_history_encrypted", columnDefinition = "TEXT")
    private String medicalHistoryEncrypted;

    // Allergies stored as encrypted JSON array
    @Lob
    @Column(name = "allergies_encrypted", columnDefinition = "TEXT")
    private String allergiesEncrypted;

    // Current medications stored as encrypted JSON array
    @Lob
    @Column(name = "current_medications_encrypted", columnDefinition = "TEXT")
    private String currentMedicationsEncrypted;

    @Embedded
    private InsuranceInfo insuranceInfo;

    @Size(max = 10, message = "Preferred language code must not exceed 10 characters")
    @Column(name = "preferred_language")
    private String preferredLanguage = "en";

    @Embedded
    private CommunicationPreferences communicationPreferences = new CommunicationPreferences();

    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified = false;

    @Column(name = "phone_verified", nullable = false)
    private Boolean phoneVerified = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @NotNull(message = "Privacy consent is required")
    @Column(name = "privacy_consent", nullable = false)
    private Boolean privacyConsent = false;

    @NotNull(message = "Terms acceptance is required")
    @Column(name = "terms_accepted", nullable = false)
    private Boolean termsAccepted = false;

    @Column(name = "consent_date")
    private LocalDateTime consentDate;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "failed_login_attempts", nullable = false)
    private Integer failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "login_count", nullable = false)
    private Integer loginCount = 0;

    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Helper methods
    public String getFullName() {
        return firstName + " " + lastName;
    }

    public int getAge() {
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    public boolean isMinor() {
        return getAge() < 18;
    }

    public boolean meetsMinimumAge(int minimumAge) {
        return getAge() >= minimumAge;
    }

    public boolean isAccountLocked() {
        return lockedUntil != null && LocalDateTime.now().isBefore(lockedUntil);
    }

    public boolean isFullyVerified() {
        return Boolean.TRUE.equals(emailVerified) && Boolean.TRUE.equals(phoneVerified);
    }

    public boolean hasCompletedRegistration() {
        return isFullyVerified() && 
               Boolean.TRUE.equals(privacyConsent) && 
               Boolean.TRUE.equals(termsAccepted) &&
               Boolean.TRUE.equals(isActive);
    }

    public boolean hasValidConsent() {
        return Boolean.TRUE.equals(privacyConsent) && 
               Boolean.TRUE.equals(termsAccepted) && 
               consentDate != null;
    }

    // Data normalization methods
    public void setEmail(String email) {
        this.email = email != null ? email.toLowerCase().trim() : null;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName != null ? firstName.trim() : null;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName != null ? lastName.trim() : null;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber != null ? phoneNumber.trim() : null;
    }

    public void setPreferredLanguage(String preferredLanguage) {
        this.preferredLanguage = preferredLanguage != null ? preferredLanguage.toLowerCase().trim() : "en";
    }

    @PrePersist
    protected void onCreate() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
        if (consentDate == null && Boolean.TRUE.equals(privacyConsent) && Boolean.TRUE.equals(termsAccepted)) {
            consentDate = LocalDateTime.now();
        }
        if (communicationPreferences == null) {
            communicationPreferences = new CommunicationPreferences();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        // Update consent date if consent changes from false to true
        if (Boolean.TRUE.equals(privacyConsent) && Boolean.TRUE.equals(termsAccepted) && consentDate == null) {
            consentDate = LocalDateTime.now();
        }
    }
} 