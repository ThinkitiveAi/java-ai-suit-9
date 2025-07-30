package com.example.session_demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyContact {

    @Size(max = 100, message = "Emergency contact name must not exceed 100 characters")
    @Column(name = "emergency_contact_name")
    private String name;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Emergency contact phone must be a valid international format")
    @Column(name = "emergency_contact_phone")
    private String phone;

    @Size(max = 50, message = "Emergency contact relationship must not exceed 50 characters")
    @Column(name = "emergency_contact_relationship")
    private String relationship;

    @Email(message = "Emergency contact email must be valid")
    @Size(max = 255, message = "Emergency contact email must not exceed 255 characters")
    @Column(name = "emergency_contact_email")
    private String email;

    // Helper methods for data normalization
    public void setName(String name) {
        this.name = name != null ? name.trim() : null;
    }

    public void setPhone(String phone) {
        this.phone = phone != null ? phone.trim() : null;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship != null ? relationship.trim().toLowerCase() : null;
    }

    public void setEmail(String email) {
        this.email = email != null ? email.toLowerCase().trim() : null;
    }

    public boolean hasContactInfo() {
        return (name != null && !name.isEmpty()) || 
               (phone != null && !phone.isEmpty()) || 
               (email != null && !email.isEmpty());
    }
} 