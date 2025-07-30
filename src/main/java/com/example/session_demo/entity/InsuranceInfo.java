package com.example.session_demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InsuranceInfo {

    @Size(max = 255, message = "Insurance provider name must not exceed 255 characters")
    @Column(name = "insurance_provider")
    private String provider;

    // This field will be encrypted by the service layer
    @Size(max = 500, message = "Policy number must not exceed 500 characters (encrypted)")
    @Column(name = "insurance_policy_number_encrypted")
    private String policyNumberEncrypted;

    @Size(max = 100, message = "Group number must not exceed 100 characters")
    @Column(name = "insurance_group_number")
    private String groupNumber;

    // This field will be encrypted by the service layer
    @Size(max = 500, message = "Member ID must not exceed 500 characters (encrypted)")
    @Column(name = "insurance_member_id_encrypted")
    private String memberIdEncrypted;

    @Column(name = "insurance_effective_date")
    private LocalDate effectiveDate;

    @Column(name = "insurance_expiry_date")
    private LocalDate expiryDate;

    // Helper methods for data normalization
    public void setProvider(String provider) {
        this.provider = provider != null ? provider.trim() : null;
    }

    public void setGroupNumber(String groupNumber) {
        this.groupNumber = groupNumber != null ? groupNumber.trim() : null;
    }

    public boolean hasInsuranceInfo() {
        return (provider != null && !provider.isEmpty()) || 
               (policyNumberEncrypted != null && !policyNumberEncrypted.isEmpty()) ||
               (memberIdEncrypted != null && !memberIdEncrypted.isEmpty());
    }

    public boolean isInsuranceActive() {
        if (effectiveDate == null && expiryDate == null) {
            return hasInsuranceInfo(); // No dates specified, assume active if info exists
        }
        
        LocalDate now = LocalDate.now();
        boolean afterEffective = effectiveDate == null || !now.isBefore(effectiveDate);
        boolean beforeExpiry = expiryDate == null || !now.isAfter(expiryDate);
        
        return afterEffective && beforeExpiry;
    }
} 