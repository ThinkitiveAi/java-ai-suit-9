package com.example.session_demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "verification_tokens", indexes = {
    @Index(name = "idx_verification_patient", columnList = "patient_id"),
    @Index(name = "idx_verification_type", columnList = "token_type"),
    @Index(name = "idx_verification_expires", columnList = "expires_at"),
    @Index(name = "idx_verification_used", columnList = "is_used"),
    @Index(name = "idx_verification_active", columnList = "is_used, expires_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "UUID", unique = true, nullable = false)
    private UUID uuid;

    @NotNull(message = "Patient is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @NotNull(message = "Token type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "token_type", nullable = false)
    private TokenType tokenType;

    @JsonIgnore
    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    // For OTP codes, this stores the encrypted value
    @JsonIgnore
    @Column(name = "token_value_encrypted")
    private String tokenValueEncrypted;

    @NotNull(message = "Expiration time is required")
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "is_used", nullable = false)
    private Boolean isUsed = false;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Min(value = 0, message = "Attempts cannot be negative")
    @Column(name = "attempts", nullable = false)
    private Integer attempts = 0;

    @Min(value = 1, message = "Max attempts must be at least 1")
    @Max(value = 10, message = "Max attempts cannot exceed 10")
    @Column(name = "max_attempts", nullable = false)
    private Integer maxAttempts = 3;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum TokenType {
        EMAIL_VERIFICATION("email_verification", 24 * 60), // 24 hours
        PHONE_VERIFICATION("phone_verification", 5), // 5 minutes  
        PASSWORD_RESET("password_reset", 60), // 1 hour
        ACCOUNT_ACTIVATION("account_activation", 24 * 60); // 24 hours

        private final String code;
        private final int defaultExpiryMinutes;

        TokenType(String code, int defaultExpiryMinutes) {
            this.code = code;
            this.defaultExpiryMinutes = defaultExpiryMinutes;
        }

        public String getCode() {
            return code;
        }

        public int getDefaultExpiryMinutes() {
            return defaultExpiryMinutes;
        }

        public LocalDateTime getDefaultExpiryTime() {
            return LocalDateTime.now().plusMinutes(defaultExpiryMinutes);
        }

        public static TokenType fromCode(String code) {
            if (code == null) {
                return null;
            }
            
            for (TokenType type : TokenType.values()) {
                if (type.code.equalsIgnoreCase(code)) {
                    return type;
                }
            }
            
            throw new IllegalArgumentException("Invalid token type code: " + code);
        }
    }

    // Helper methods
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !isUsed && !isExpired() && attempts < maxAttempts;
    }

    public boolean canAttempt() {
        return !isUsed && !isExpired() && attempts < maxAttempts;
    }

    public void incrementAttempts() {
        this.attempts++;
    }

    public void markAsUsed() {
        this.isUsed = true;
        this.usedAt = LocalDateTime.now();
    }

    public boolean hasExceededMaxAttempts() {
        return attempts >= maxAttempts;
    }

    public int getRemainingAttempts() {
        return Math.max(0, maxAttempts - attempts);
    }

    public long getMinutesUntilExpiry() {
        if (isExpired()) {
            return 0;
        }
        return java.time.Duration.between(LocalDateTime.now(), expiresAt).toMinutes();
    }

    @PrePersist
    protected void onCreate() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
        if (maxAttempts == null) {
            maxAttempts = tokenType != null ? 
                (tokenType == TokenType.PHONE_VERIFICATION ? 3 : 5) : 3;
        }
    }
} 