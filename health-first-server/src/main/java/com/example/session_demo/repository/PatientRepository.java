package com.example.session_demo.repository;

import com.example.session_demo.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    // Find by unique identifiers
    Optional<Patient> findByUuid(UUID uuid);
    Optional<Patient> findByEmail(String email);
    Optional<Patient> findByPhoneNumber(String phoneNumber);

    // Check existence by unique fields
    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);
    boolean existsByEmailOrPhoneNumber(String email, String phoneNumber);

    // Find by verification status
    List<Patient> findByEmailVerifiedAndPhoneVerified(Boolean emailVerified, Boolean phoneVerified);
    List<Patient> findByEmailVerifiedFalseOrPhoneVerifiedFalse();

    @Query("SELECT p FROM Patient p WHERE " +
           "(p.emailVerified = false OR p.phoneVerified = false) AND " +
           "p.createdAt < :cutoffTime")
    List<Patient> findUnverifiedPatientsOlderThan(@Param("cutoffTime") LocalDateTime cutoffTime);

    // Find by account status
    List<Patient> findByIsActive(Boolean isActive);
    List<Patient> findByIsActiveTrueAndEmailVerifiedTrueAndPhoneVerifiedTrue();

    // Find patients with consent
    List<Patient> findByPrivacyConsentAndTermsAccepted(Boolean privacyConsent, Boolean termsAccepted);

    @Query("SELECT p FROM Patient p WHERE " +
           "p.privacyConsent = true AND p.termsAccepted = true AND " +
           "p.consentDate IS NOT NULL")
    List<Patient> findPatientsWithValidConsent();

    // Find locked accounts
    @Query("SELECT p FROM Patient p WHERE p.lockedUntil IS NOT NULL AND p.lockedUntil > :currentTime")
    List<Patient> findLockedPatients(@Param("currentTime") LocalDateTime currentTime);

    // Find patients by failed login attempts
    List<Patient> findByFailedLoginAttemptsGreaterThan(Integer attempts);

    // Find inactive patients
    @Query("SELECT p FROM Patient p WHERE " +
           "p.isActive = true AND " +
           "(p.lastLogin IS NULL OR p.lastLogin < :inactiveThreshold)")
    List<Patient> findInactivePatients(@Param("inactiveThreshold") LocalDateTime inactiveThreshold);

    // Update operations
    @Modifying
    @Query("UPDATE Patient p SET p.emailVerified = true WHERE p.uuid = :uuid")
    int markEmailAsVerified(@Param("uuid") UUID uuid);

    @Modifying
    @Query("UPDATE Patient p SET p.phoneVerified = true WHERE p.uuid = :uuid")
    int markPhoneAsVerified(@Param("uuid") UUID uuid);

    @Modifying
    @Query("UPDATE Patient p SET p.lastLogin = :loginTime, " +
           "p.loginCount = p.loginCount + 1, " +
           "p.failedLoginAttempts = 0 WHERE p.uuid = :uuid")
    int updateLastLogin(@Param("uuid") UUID uuid, @Param("loginTime") LocalDateTime loginTime);

    @Modifying
    @Query("UPDATE Patient p SET p.failedLoginAttempts = p.failedLoginAttempts + 1 WHERE p.uuid = :uuid")
    int incrementFailedLoginAttempts(@Param("uuid") UUID uuid);

    @Modifying
    @Query("UPDATE Patient p SET p.lockedUntil = :lockUntil WHERE p.uuid = :uuid")
    int lockAccount(@Param("uuid") UUID uuid, @Param("lockUntil") LocalDateTime lockUntil);

    @Modifying
    @Query("UPDATE Patient p SET p.lockedUntil = NULL, p.failedLoginAttempts = 0 WHERE p.uuid = :uuid")
    int unlockAccount(@Param("uuid") UUID uuid);

    @Modifying
    @Query("UPDATE Patient p SET p.isActive = :active WHERE p.uuid = :uuid")
    int updateActiveStatus(@Param("uuid") UUID uuid, @Param("active") Boolean active);

    @Modifying
    @Query("UPDATE Patient p SET p.passwordHash = :passwordHash, " +
           "p.passwordChangedAt = :changedAt WHERE p.uuid = :uuid")
    int updatePassword(@Param("uuid") UUID uuid, 
                      @Param("passwordHash") String passwordHash, 
                      @Param("changedAt") LocalDateTime changedAt);

    // Statistics and reporting queries
    @Query("SELECT COUNT(p) FROM Patient p WHERE p.createdAt >= :fromDate")
    long countPatientsRegisteredSince(@Param("fromDate") LocalDateTime fromDate);

    @Query("SELECT COUNT(p) FROM Patient p WHERE " +
           "p.emailVerified = true AND p.phoneVerified = true")
    long countFullyVerifiedPatients();

    @Query("SELECT COUNT(p) FROM Patient p WHERE p.isActive = true")
    long countActivePatients();

    @Query("SELECT COUNT(p) FROM Patient p WHERE " +
           "p.lockedUntil IS NOT NULL AND p.lockedUntil > :currentTime")
    long countLockedPatients(@Param("currentTime") LocalDateTime currentTime);

    // Age-based queries
    @Query("SELECT p FROM Patient p WHERE " +
           "YEAR(CURRENT_DATE) - YEAR(p.dateOfBirth) < 18")
    List<Patient> findMinorPatients();

    @Query("SELECT p FROM Patient p WHERE " +
           "YEAR(CURRENT_DATE) - YEAR(p.dateOfBirth) >= :minimumAge")
    List<Patient> findPatientsOlderThan(@Param("minimumAge") int minimumAge);

    // Cleanup queries for maintenance
    @Query("SELECT p FROM Patient p WHERE " +
           "p.isActive = false AND " +
           "p.updatedAt < :cutoffDate")
    List<Patient> findInactivePatientsForCleanup(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Modifying
    @Query("DELETE FROM Patient p WHERE " +
           "p.isActive = false AND " +
           "p.emailVerified = false AND " +
           "p.phoneVerified = false AND " +
           "p.createdAt < :cutoffDate")
    int deleteUnverifiedInactivePatients(@Param("cutoffDate") LocalDateTime cutoffDate);
} 