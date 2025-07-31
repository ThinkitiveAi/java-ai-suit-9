package com.example.session_demo.repository;

import com.example.session_demo.entity.RefreshToken;
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
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Find refresh token by its hash
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Find all valid refresh tokens for a provider
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.provider.uuid = :providerUuid AND rt.isRevoked = false AND rt.expiresAt > :now")
    List<RefreshToken> findValidTokensByProviderUuid(@Param("providerUuid") UUID providerUuid, @Param("now") LocalDateTime now);

    /**
     * Count active sessions for a provider
     */
    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.provider.uuid = :providerUuid AND rt.isRevoked = false AND rt.expiresAt > :now")
    long countActiveSessionsByProviderUuid(@Param("providerUuid") UUID providerUuid, @Param("now") LocalDateTime now);

    /**
     * Find expired tokens
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.expiresAt < :now")
    List<RefreshToken> findExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * Revoke all tokens for a provider
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.isRevoked = true WHERE rt.provider.uuid = :providerUuid")
    void revokeAllTokensByProviderUuid(@Param("providerUuid") UUID providerUuid);

    /**
     * Revoke a specific token
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.isRevoked = true WHERE rt.tokenHash = :tokenHash")
    void revokeTokenByHash(@Param("tokenHash") String tokenHash);

    /**
     * Delete expired tokens
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * Find tokens by device info for a provider
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.provider.uuid = :providerUuid AND rt.deviceInfo = :deviceInfo AND rt.isRevoked = false")
    List<RefreshToken> findByProviderUuidAndDeviceInfo(@Param("providerUuid") UUID providerUuid, @Param("deviceInfo") String deviceInfo);
} 