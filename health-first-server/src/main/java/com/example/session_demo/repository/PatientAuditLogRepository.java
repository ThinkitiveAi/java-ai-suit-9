package com.example.session_demo.repository;

import com.example.session_demo.entity.Patient;
import com.example.session_demo.entity.PatientAuditLog;
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
public interface PatientAuditLogRepository extends JpaRepository<PatientAuditLog, Long> {

    // Find by unique identifiers
    Optional<PatientAuditLog> findByUuid(UUID uuid);

    // Find by patient
    List<PatientAuditLog> findByPatient(Patient patient);
    List<PatientAuditLog> findByPatientOrderByCreatedAtDesc(Patient patient);
    List<PatientAuditLog> findByPatientUuid(UUID patientUuid);

    // Find by action type
    List<PatientAuditLog> findByActionType(PatientAuditLog.ActionType actionType);
    List<PatientAuditLog> findByActionTypeAndPatient(PatientAuditLog.ActionType actionType, Patient patient);

    // Find by success status
    List<PatientAuditLog> findBySuccess(Boolean success);
    List<PatientAuditLog> findBySuccessAndPatient(Boolean success, Patient patient);

    // Find security events
    @Query("SELECT pal FROM PatientAuditLog pal WHERE " +
           "pal.actionType IN ('SUSPICIOUS_ACTIVITY_DETECTED', 'RATE_LIMIT_EXCEEDED', " +
           "'UNAUTHORIZED_ACCESS_ATTEMPT', 'PATIENT_LOGIN_FAILED', 'ACCOUNT_LOCKOUT')")
    List<PatientAuditLog> findSecurityEvents();

    @Query("SELECT pal FROM PatientAuditLog pal WHERE " +
           "pal.patient = :patient AND " +
           "pal.actionType IN ('SUSPICIOUS_ACTIVITY_DETECTED', 'RATE_LIMIT_EXCEEDED', " +
           "'UNAUTHORIZED_ACCESS_ATTEMPT', 'PATIENT_LOGIN_FAILED', 'ACCOUNT_LOCKOUT')")
    List<PatientAuditLog> findSecurityEventsForPatient(@Param("patient") Patient patient);

    // Find by sensitive data access
    List<PatientAuditLog> findBySensitiveDataAccessed(Boolean sensitiveDataAccessed);

    @Query("SELECT pal FROM PatientAuditLog pal WHERE " +
           "pal.sensitiveDataAccessed = true AND " +
           "pal.patient = :patient")
    List<PatientAuditLog> findSensitiveDataAccessForPatient(@Param("patient") Patient patient);

    // Find by IP address
    List<PatientAuditLog> findByIpAddress(String ipAddress);

    @Query("SELECT pal FROM PatientAuditLog pal WHERE " +
           "pal.ipAddress = :ipAddress AND " +
           "pal.createdAt >= :since")
    List<PatientAuditLog> findByIpAddressSince(@Param("ipAddress") String ipAddress, 
                                              @Param("since") LocalDateTime since);

    // Find by actor type
    List<PatientAuditLog> findByActorType(String actorType);
    List<PatientAuditLog> findByActorTypeAndActorId(String actorType, String actorId);

    // Find by date range
    @Query("SELECT pal FROM PatientAuditLog pal WHERE " +
           "pal.createdAt >= :startDate AND pal.createdAt <= :endDate")
    List<PatientAuditLog> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                                         @Param("endDate") LocalDateTime endDate);

    @Query("SELECT pal FROM PatientAuditLog pal WHERE " +
           "pal.patient = :patient AND " +
           "pal.createdAt >= :startDate AND pal.createdAt <= :endDate")
    List<PatientAuditLog> findByPatientAndDateRange(@Param("patient") Patient patient,
                                                   @Param("startDate") LocalDateTime startDate, 
                                                   @Param("endDate") LocalDateTime endDate);

    // Find failed attempts
    @Query("SELECT pal FROM PatientAuditLog pal WHERE " +
           "pal.success = false AND pal.createdAt >= :since")
    List<PatientAuditLog> findFailedAttemptsSince(@Param("since") LocalDateTime since);

    @Query("SELECT pal FROM PatientAuditLog pal WHERE " +
           "pal.success = false AND " +
           "pal.patient = :patient AND " +
           "pal.createdAt >= :since")
    List<PatientAuditLog> findFailedAttemptsForPatientSince(@Param("patient") Patient patient,
                                                           @Param("since") LocalDateTime since);

    // Find by session ID
    List<PatientAuditLog> findBySessionId(String sessionId);

    // Statistics queries
    @Query("SELECT COUNT(pal) FROM PatientAuditLog pal WHERE " +
           "pal.actionType = :actionType AND pal.createdAt >= :since")
    long countByActionTypeSince(@Param("actionType") PatientAuditLog.ActionType actionType, 
                               @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(pal) FROM PatientAuditLog pal WHERE " +
           "pal.patient = :patient AND pal.createdAt >= :since")
    long countByPatientSince(@Param("patient") Patient patient, 
                            @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(pal) FROM PatientAuditLog pal WHERE " +
           "pal.sensitiveDataAccessed = true AND pal.createdAt >= :since")
    long countSensitiveDataAccessSince(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(pal) FROM PatientAuditLog pal WHERE " +
           "pal.success = false AND pal.createdAt >= :since")
    long countFailedAttemptsSince(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(pal) FROM PatientAuditLog pal WHERE " +
           "pal.ipAddress = :ipAddress AND pal.createdAt >= :since")
    long countByIpAddressSince(@Param("ipAddress") String ipAddress, 
                              @Param("since") LocalDateTime since);

    // HIPAA compliance queries
    @Query("SELECT pal FROM PatientAuditLog pal WHERE " +
           "pal.patient = :patient AND " +
           "pal.sensitiveDataAccessed = true " +
           "ORDER BY pal.createdAt DESC")
    List<PatientAuditLog> findSensitiveDataAccessHistoryForPatient(@Param("patient") Patient patient);

    @Query("SELECT pal FROM PatientAuditLog pal WHERE " +
           "pal.actionType IN ('PATIENT_DATA_VIEW', 'PATIENT_DATA_UPDATE', 'PATIENT_DATA_EXPORT', " +
           "'MEDICAL_HISTORY_VIEW', 'MEDICAL_HISTORY_UPDATE', 'INSURANCE_INFO_VIEW', 'INSURANCE_INFO_UPDATE') " +
           "AND pal.createdAt >= :since")
    List<PatientAuditLog> findDataAccessEventsSince(@Param("since") LocalDateTime since);

    // Cleanup queries
    @Modifying
    @Query("DELETE FROM PatientAuditLog pal WHERE pal.createdAt < :cutoffDate")
    int deleteOldAuditLogs(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Query("SELECT COUNT(pal) FROM PatientAuditLog pal WHERE pal.createdAt < :cutoffDate")
    long countOldAuditLogs(@Param("cutoffDate") LocalDateTime cutoffDate);

    // Recent activity queries
    @Query("SELECT pal FROM PatientAuditLog pal WHERE " +
           "pal.patient = :patient " +
           "ORDER BY pal.createdAt DESC " +
           "LIMIT :limit")
    List<PatientAuditLog> findRecentActivityForPatient(@Param("patient") Patient patient, 
                                                      @Param("limit") int limit);

    @Query("SELECT pal FROM PatientAuditLog pal WHERE " +
           "pal.createdAt >= :since " +
           "ORDER BY pal.createdAt DESC")
    List<PatientAuditLog> findRecentActivity(@Param("since") LocalDateTime since);

    // Suspicious activity detection
    @Query("SELECT pal FROM PatientAuditLog pal WHERE " +
           "pal.ipAddress = :ipAddress AND " +
           "pal.success = false AND " +
           "pal.createdAt >= :since")
    List<PatientAuditLog> findFailedAttemptsFromIpSince(@Param("ipAddress") String ipAddress, 
                                                       @Param("since") LocalDateTime since);

    @Query("SELECT DISTINCT pal.ipAddress FROM PatientAuditLog pal WHERE " +
           "pal.patient = :patient AND " +
           "pal.createdAt >= :since")
    List<String> findDistinctIpAddressesForPatient(@Param("patient") Patient patient, 
                                                  @Param("since") LocalDateTime since);

    // Reporting queries for compliance
    @Query("SELECT pal FROM PatientAuditLog pal WHERE " +
           "pal.actionType IN ('PRIVACY_CONSENT_GIVEN', 'PRIVACY_CONSENT_WITHDRAWN', " +
           "'TERMS_ACCEPTED', 'MARKETING_CONSENT_CHANGED') AND " +
           "pal.createdAt >= :since")
    List<PatientAuditLog> findConsentChangeEventsSince(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(DISTINCT pal.patient.uuid) FROM PatientAuditLog pal WHERE " +
           "pal.sensitiveDataAccessed = true AND " +
           "pal.createdAt >= :since")
    long countDistinctPatientsWithSensitiveDataAccessSince(@Param("since") LocalDateTime since);
} 