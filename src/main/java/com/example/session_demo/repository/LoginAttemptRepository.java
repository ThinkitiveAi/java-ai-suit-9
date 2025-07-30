package com.example.session_demo.repository;

import com.example.session_demo.entity.LoginAttempt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, UUID> {

    /**
     * Count failed attempts by identifier and IP in time window
     */
    @Query("SELECT COUNT(la) FROM LoginAttempt la WHERE la.identifier = :identifier AND la.ipAddress = :ipAddress AND la.attemptType IN ('FAILED', 'LOCKED') AND la.createdAt > :since")
    long countFailedAttemptsByIdentifierAndIp(@Param("identifier") String identifier, @Param("ipAddress") String ipAddress, @Param("since") LocalDateTime since);

    /**
     * Count failed attempts by IP in time window
     */
    @Query("SELECT COUNT(la) FROM LoginAttempt la WHERE la.ipAddress = :ipAddress AND la.attemptType IN ('FAILED', 'LOCKED') AND la.createdAt > :since")
    long countFailedAttemptsByIp(@Param("ipAddress") String ipAddress, @Param("since") LocalDateTime since);

    /**
     * Count failed attempts by identifier in time window
     */
    @Query("SELECT COUNT(la) FROM LoginAttempt la WHERE la.identifier = :identifier AND la.attemptType IN ('FAILED', 'LOCKED') AND la.createdAt > :since")
    long countFailedAttemptsByIdentifier(@Param("identifier") String identifier, @Param("since") LocalDateTime since);

    /**
     * Find recent login attempts by identifier
     */
    @Query("SELECT la FROM LoginAttempt la WHERE la.identifier = :identifier ORDER BY la.createdAt DESC")
    List<LoginAttempt> findRecentAttemptsByIdentifier(@Param("identifier") String identifier, Pageable pageable);

    /**
     * Find login attempts by provider
     */
    @Query("SELECT la FROM LoginAttempt la WHERE la.provider.uuid = :providerUuid ORDER BY la.createdAt DESC")
    Page<LoginAttempt> findByProviderUuid(@Param("providerUuid") UUID providerUuid, Pageable pageable);

    /**
     * Find suspicious activity (multiple failed attempts from different IPs)
     */
    @Query("SELECT la.identifier, COUNT(DISTINCT la.ipAddress) as ipCount FROM LoginAttempt la WHERE la.attemptType IN ('FAILED', 'LOCKED') AND la.createdAt > :since GROUP BY la.identifier HAVING COUNT(DISTINCT la.ipAddress) > 2")
    List<Object[]> findSuspiciousActivity(@Param("since") LocalDateTime since);

    /**
     * Get last successful login for a provider
     */
    @Query("SELECT la FROM LoginAttempt la WHERE la.provider.uuid = :providerUuid AND la.attemptType = 'SUCCESS' ORDER BY la.createdAt DESC")
    List<LoginAttempt> findLastSuccessfulLogin(@Param("providerUuid") UUID providerUuid, Pageable pageable);

    /**
     * Clean up old login attempts (older than specified date)
     */
    @Query("DELETE FROM LoginAttempt la WHERE la.createdAt < :before")
    void deleteOldAttempts(@Param("before") LocalDateTime before);
} 