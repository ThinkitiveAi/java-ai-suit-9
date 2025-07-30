package com.example.session_demo.repository;

import com.example.session_demo.entity.Patient;
import com.example.session_demo.entity.VerificationToken;
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
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    // Find by unique identifiers
    Optional<VerificationToken> findByUuid(UUID uuid);
    Optional<VerificationToken> findByTokenHash(String tokenHash);

    // Find by patient and token type
    Optional<VerificationToken> findByPatientAndTokenType(Patient patient, VerificationToken.TokenType tokenType);
    Optional<VerificationToken> findByPatientUuidAndTokenType(UUID patientUuid, VerificationToken.TokenType tokenType);

    // Find active/valid tokens
    @Query("SELECT vt FROM VerificationToken vt WHERE " +
           "vt.patient = :patient AND " +
           "vt.tokenType = :tokenType AND " +
           "vt.isUsed = false AND " +
           "vt.expiresAt > :currentTime AND " +
           "vt.attempts < vt.maxAttempts")
    Optional<VerificationToken> findActiveTokenByPatientAndType(
            @Param("patient") Patient patient,
            @Param("tokenType") VerificationToken.TokenType tokenType,
            @Param("currentTime") LocalDateTime currentTime);

    @Query("SELECT vt FROM VerificationToken vt WHERE " +
           "vt.patient.uuid = :patientUuid AND " +
           "vt.tokenType = :tokenType AND " +
           "vt.isUsed = false AND " +
           "vt.expiresAt > :currentTime AND " +
           "vt.attempts < vt.maxAttempts")
    Optional<VerificationToken> findActiveTokenByPatientUuidAndType(
            @Param("patientUuid") UUID patientUuid,
            @Param("tokenType") VerificationToken.TokenType tokenType,
            @Param("currentTime") LocalDateTime currentTime);

    // Find all tokens for a patient
    List<VerificationToken> findByPatient(Patient patient);
    List<VerificationToken> findByPatientUuid(UUID patientUuid);

    // Find tokens by status
    List<VerificationToken> findByIsUsed(Boolean isUsed);
    List<VerificationToken> findByTokenType(VerificationToken.TokenType tokenType);

    // Find expired tokens
    @Query("SELECT vt FROM VerificationToken vt WHERE vt.expiresAt < :currentTime")
    List<VerificationToken> findExpiredTokens(@Param("currentTime") LocalDateTime currentTime);

    @Query("SELECT vt FROM VerificationToken vt WHERE " +
           "vt.expiresAt < :currentTime AND vt.isUsed = false")
    List<VerificationToken> findExpiredUnusedTokens(@Param("currentTime") LocalDateTime currentTime);

    // Find tokens with too many attempts
    @Query("SELECT vt FROM VerificationToken vt WHERE vt.attempts >= vt.maxAttempts")
    List<VerificationToken> findTokensWithExceededAttempts();

    // Find tokens by IP address (for security tracking)
    List<VerificationToken> findByIpAddress(String ipAddress);

    @Query("SELECT vt FROM VerificationToken vt WHERE " +
           "vt.ipAddress = :ipAddress AND " +
           "vt.createdAt >= :since")
    List<VerificationToken> findByIpAddressSince(@Param("ipAddress") String ipAddress, 
                                                @Param("since") LocalDateTime since);

    // Update operations
    @Modifying
    @Query("UPDATE VerificationToken vt SET " +
           "vt.attempts = vt.attempts + 1 WHERE vt.uuid = :uuid")
    int incrementAttempts(@Param("uuid") UUID uuid);

    @Modifying
    @Query("UPDATE VerificationToken vt SET " +
           "vt.isUsed = true, vt.usedAt = :usedAt WHERE vt.uuid = :uuid")
    int markAsUsed(@Param("uuid") UUID uuid, @Param("usedAt") LocalDateTime usedAt);

    @Modifying
    @Query("UPDATE VerificationToken vt SET " +
           "vt.isUsed = true WHERE vt.patient = :patient AND vt.tokenType = :tokenType")
    int markAllTokensAsUsedForPatientAndType(@Param("patient") Patient patient, 
                                           @Param("tokenType") VerificationToken.TokenType tokenType);

    // Cleanup operations
    @Modifying
    @Query("DELETE FROM VerificationToken vt WHERE vt.expiresAt < :cutoffTime")
    int deleteExpiredTokens(@Param("cutoffTime") LocalDateTime cutoffTime);

    @Modifying
    @Query("DELETE FROM VerificationToken vt WHERE " +
           "vt.isUsed = true AND vt.usedAt < :cutoffTime")
    int deleteUsedTokensOlderThan(@Param("cutoffTime") LocalDateTime cutoffTime);

    @Modifying
    @Query("DELETE FROM VerificationToken vt WHERE " +
           "vt.patient = :patient AND vt.tokenType = :tokenType")
    int deleteTokensForPatientAndType(@Param("patient") Patient patient, 
                                    @Param("tokenType") VerificationToken.TokenType tokenType);

    // Statistics queries
    @Query("SELECT COUNT(vt) FROM VerificationToken vt WHERE " +
           "vt.tokenType = :tokenType AND vt.createdAt >= :since")
    long countTokensByTypeSince(@Param("tokenType") VerificationToken.TokenType tokenType, 
                               @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(vt) FROM VerificationToken vt WHERE " +
           "vt.patient.uuid = :patientUuid AND " +
           "vt.tokenType = :tokenType AND " +
           "vt.createdAt >= :since")
    long countTokensForPatientSince(@Param("patientUuid") UUID patientUuid,
                                   @Param("tokenType") VerificationToken.TokenType tokenType,
                                   @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(vt) FROM VerificationToken vt WHERE " +
           "vt.ipAddress = :ipAddress AND " +
           "vt.tokenType = :tokenType AND " +
           "vt.createdAt >= :since")
    long countTokensByIpAndTypeSince(@Param("ipAddress") String ipAddress,
                                    @Param("tokenType") VerificationToken.TokenType tokenType,
                                    @Param("since") LocalDateTime since);

    // Rate limiting queries
    @Query("SELECT COUNT(vt) FROM VerificationToken vt WHERE " +
           "vt.patient.uuid = :patientUuid AND " +
           "vt.tokenType = :tokenType AND " +
           "vt.createdAt >= :since")
    long countRecentTokensForPatient(@Param("patientUuid") UUID patientUuid,
                                   @Param("tokenType") VerificationToken.TokenType tokenType,
                                   @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(vt) FROM VerificationToken vt WHERE " +
           "vt.ipAddress = :ipAddress AND " +
           "vt.createdAt >= :since")
    long countRecentTokensByIp(@Param("ipAddress") String ipAddress, 
                              @Param("since") LocalDateTime since);
} 