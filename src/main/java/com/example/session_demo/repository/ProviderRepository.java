package com.example.session_demo.repository;

import com.example.session_demo.entity.Provider;
import com.example.session_demo.enums.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProviderRepository extends JpaRepository<Provider, UUID> {

    /**
     * Find provider by email address
     */
    Optional<Provider> findByEmail(String email);
    Optional<Provider> findByUuid(UUID uuid);

    /**
     * Find provider by phone number
     */
    Optional<Provider> findByPhoneNumber(String phoneNumber);

    /**
     * Find provider by license number
     */
    Optional<Provider> findByLicenseNumber(String licenseNumber);

    /**
     * Find provider by email verification token
     */
    Optional<Provider> findByEmailVerificationToken(String token);

    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Check if phone number exists
     */
    boolean existsByPhoneNumber(String phoneNumber);

    /**
     * Check if license number exists
     */
    boolean existsByLicenseNumber(String licenseNumber);

    /**
     * Find providers by verification status
     */
    List<Provider> findByVerificationStatus(VerificationStatus status);

    /**
     * Find active providers
     */
    List<Provider> findByIsActiveTrue();

    /**
     * Find providers with expired verification tokens
     */
    @Query("SELECT p FROM Provider p WHERE p.emailVerificationToken IS NOT NULL " +
           "AND p.emailVerificationTokenExpiry < :currentTime")
    List<Provider> findProvidersWithExpiredTokens(@Param("currentTime") LocalDateTime currentTime);

    /**
     * Count providers by verification status
     */
    long countByVerificationStatus(VerificationStatus status);

    /**
     * Find providers created between dates
     */
    @Query("SELECT p FROM Provider p WHERE p.createdAt BETWEEN :startDate AND :endDate")
    List<Provider> findProvidersCreatedBetween(@Param("startDate") LocalDateTime startDate, 
                                             @Param("endDate") LocalDateTime endDate);
} 