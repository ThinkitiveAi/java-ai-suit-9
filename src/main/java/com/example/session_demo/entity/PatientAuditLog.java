package com.example.session_demo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "patient_audit_logs", indexes = {
    @Index(name = "idx_audit_patient", columnList = "patient_id"),
    @Index(name = "idx_audit_action", columnList = "action_type"),
    @Index(name = "idx_audit_created", columnList = "created_at"),
    @Index(name = "idx_audit_sensitive", columnList = "sensitive_data_accessed"),
    @Index(name = "idx_audit_success", columnList = "success"),
    @Index(name = "idx_audit_ip", columnList = "ip_address")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatientAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "UUID", unique = true, nullable = false)
    private UUID uuid;

    // Nullable to support system-wide audit logs
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @NotNull(message = "Action type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false)
    private ActionType actionType;

    // Encrypted JSON containing action details
    @Lob
    @Column(name = "action_details_encrypted", columnDefinition = "TEXT")
    private String actionDetailsEncrypted;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "success", nullable = false)
    private Boolean success = true;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "sensitive_data_accessed", nullable = false)
    private Boolean sensitiveDataAccessed = false;

    // For tracking who performed the action (system, patient, provider, admin)
    @Column(name = "actor_type")
    private String actorType = "SYSTEM";

    @Column(name = "actor_id")
    private String actorId;

    @Column(name = "session_id")
    private String sessionId;

    // Request details for API calls
    @Column(name = "request_method")
    private String requestMethod;

    @Column(name = "request_uri")
    private String requestUri;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum ActionType {
        // Registration and account management
        PATIENT_REGISTRATION("patient_registration"),
        PATIENT_LOGIN("patient_login"),
        PATIENT_LOGOUT("patient_logout"),
        PATIENT_LOGIN_FAILED("patient_login_failed"),
        PASSWORD_CHANGE("password_change"),
        PASSWORD_RESET_REQUEST("password_reset_request"),
        PASSWORD_RESET_COMPLETE("password_reset_complete"),
        ACCOUNT_ACTIVATION("account_activation"),
        ACCOUNT_DEACTIVATION("account_deactivation"),
        ACCOUNT_LOCKOUT("account_lockout"),
        
        // Verification actions
        EMAIL_VERIFICATION_SENT("email_verification_sent"),
        EMAIL_VERIFICATION_COMPLETED("email_verification_completed"),
        PHONE_VERIFICATION_SENT("phone_verification_sent"),
        PHONE_VERIFICATION_COMPLETED("phone_verification_completed"),
        VERIFICATION_TOKEN_EXPIRED("verification_token_expired"),
        
        // Data access and modifications
        PATIENT_DATA_VIEW("patient_data_view"),
        PATIENT_DATA_UPDATE("patient_data_update"),
        PATIENT_DATA_EXPORT("patient_data_export"),
        PATIENT_DATA_DELETE("patient_data_delete"),
        MEDICAL_HISTORY_VIEW("medical_history_view"),
        MEDICAL_HISTORY_UPDATE("medical_history_update"),
        INSURANCE_INFO_VIEW("insurance_info_view"),
        INSURANCE_INFO_UPDATE("insurance_info_update"),
        
        // Privacy and consent
        PRIVACY_CONSENT_GIVEN("privacy_consent_given"),
        PRIVACY_CONSENT_WITHDRAWN("privacy_consent_withdrawn"),
        TERMS_ACCEPTED("terms_accepted"),
        MARKETING_CONSENT_CHANGED("marketing_consent_changed"),
        
        // Communication
        EMAIL_SENT("email_sent"),
        SMS_SENT("sms_sent"),
        COMMUNICATION_PREFERENCES_UPDATED("communication_preferences_updated"),
        
        // Security events
        SUSPICIOUS_ACTIVITY_DETECTED("suspicious_activity_detected"),
        RATE_LIMIT_EXCEEDED("rate_limit_exceeded"),
        UNAUTHORIZED_ACCESS_ATTEMPT("unauthorized_access_attempt"),
        DATA_ENCRYPTION_PERFORMED("data_encryption_performed"),
        DATA_DECRYPTION_PERFORMED("data_decryption_performed"),
        
        // System events
        SYSTEM_BACKUP("system_backup"),
        SYSTEM_RESTORE("system_restore"),
        DATABASE_MIGRATION("database_migration"),
        AUDIT_LOG_CLEANUP("audit_log_cleanup");

        private final String code;

        ActionType(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }

        public static ActionType fromCode(String code) {
            if (code == null) {
                return null;
            }
            
            for (ActionType type : ActionType.values()) {
                if (type.code.equalsIgnoreCase(code)) {
                    return type;
                }
            }
            
            throw new IllegalArgumentException("Invalid action type code: " + code);
        }

        // Helper methods to determine if action involves sensitive data
        public boolean involvesSensitiveData() {
            return this == MEDICAL_HISTORY_VIEW || 
                   this == MEDICAL_HISTORY_UPDATE ||
                   this == INSURANCE_INFO_VIEW ||
                   this == INSURANCE_INFO_UPDATE ||
                   this == PATIENT_DATA_VIEW ||
                   this == PATIENT_DATA_UPDATE ||
                   this == PATIENT_DATA_EXPORT ||
                   this == DATA_DECRYPTION_PERFORMED;
        }

        public boolean isSecurityEvent() {
            return this == SUSPICIOUS_ACTIVITY_DETECTED ||
                   this == RATE_LIMIT_EXCEEDED ||
                   this == UNAUTHORIZED_ACCESS_ATTEMPT ||
                   this == PATIENT_LOGIN_FAILED ||
                   this == ACCOUNT_LOCKOUT;
        }
    }

    // Helper methods
    public boolean isSuccessful() {
        return Boolean.TRUE.equals(success);
    }

    public boolean involvesSensitiveData() {
        return Boolean.TRUE.equals(sensitiveDataAccessed) || 
               (actionType != null && actionType.involvesSensitiveData());
    }

    public boolean isSecurityEvent() {
        return actionType != null && actionType.isSecurityEvent();
    }

    public boolean isPatientAction() {
        return "PATIENT".equals(actorType);
    }

    public boolean isSystemAction() {
        return "SYSTEM".equals(actorType);
    }

    @PrePersist
    protected void onCreate() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
        
        // Auto-set sensitive data flag based on action type
        if (actionType != null && actionType.involvesSensitiveData()) {
            sensitiveDataAccessed = true;
        }
    }
} 