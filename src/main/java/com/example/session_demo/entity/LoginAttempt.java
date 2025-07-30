package com.example.session_demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "login_attempts", indexes = {
    @Index(name = "idx_login_attempt_provider", columnList = "provider_id"),
    @Index(name = "idx_login_attempt_identifier", columnList = "identifier"),
    @Index(name = "idx_login_attempt_ip", columnList = "ip_address"),
    @Index(name = "idx_login_attempt_created", columnList = "created_at"),
    @Index(name = "idx_login_attempt_type", columnList = "attempt_type")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class LoginAttempt {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id")
    private Provider provider;

    @Column(name = "identifier", nullable = false)
    private String identifier; // email or phone used for login attempt

    @Column(name = "ip_address", nullable = false)
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Enumerated(EnumType.STRING)
    @Column(name = "attempt_type", nullable = false)
    private AttemptType attemptType;

    @Column(name = "failure_reason")
    private String failureReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum AttemptType {
        SUCCESS,
        FAILED,
        LOCKED,
        RATE_LIMITED,
        ACCOUNT_DISABLED,
        EMAIL_NOT_VERIFIED
    }

    public enum FailureReason {
        INVALID_PASSWORD,
        ACCOUNT_NOT_FOUND,
        ACCOUNT_LOCKED,
        ACCOUNT_DISABLED,
        EMAIL_NOT_VERIFIED,
        RATE_LIMITED,
        TOO_MANY_ATTEMPTS,
        INVALID_CREDENTIALS
    }
} 