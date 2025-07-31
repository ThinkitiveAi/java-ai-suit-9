package com.example.session_demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommunicationPreferences {

    @Column(name = "email_notifications", nullable = false)
    private Boolean emailNotifications = true;

    @Column(name = "sms_notifications", nullable = false)
    private Boolean smsNotifications = false;

    @Column(name = "marketing_emails", nullable = false)
    private Boolean marketingEmails = false;

    @Column(name = "appointment_reminders", nullable = false)
    private Boolean appointmentReminders = true;

    // Convenience methods
    public boolean allowsEmailCommunication() {
        return Boolean.TRUE.equals(emailNotifications);
    }

    public boolean allowsSmsCommunication() {
        return Boolean.TRUE.equals(smsNotifications);
    }

    public boolean acceptsMarketingEmails() {
        return Boolean.TRUE.equals(marketingEmails);
    }

    public boolean wantsAppointmentReminders() {
        return Boolean.TRUE.equals(appointmentReminders);
    }

    // Helper method to check if any communication is allowed
    public boolean hasAnyCommunicationEnabled() {
        return allowsEmailCommunication() || allowsSmsCommunication();
    }
} 