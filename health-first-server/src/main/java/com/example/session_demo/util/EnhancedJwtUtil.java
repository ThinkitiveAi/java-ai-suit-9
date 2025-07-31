package com.example.session_demo.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class EnhancedJwtUtil {

    @Value("${jwt.secret:defaultSecretKeyForDevelopmentOnly}")
    private String secret;

    @Value("${jwt.access.expiration:3600000}") // 1 hour in milliseconds
    private long accessTokenExpiration;

    @Value("${jwt.refresh.expiration:604800000}") // 7 days in milliseconds
    private long refreshTokenExpiration;

    @Value("${jwt.refresh.remember-me.expiration:2592000000}") // 30 days in milliseconds
    private long rememberMeExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateAccessToken(UUID uuid, String email, String firstName, String lastName, String specialization, String verificationStatus) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("uuid", uuid.toString());
        claims.put("email", email);
        claims.put("firstName", firstName);
        claims.put("lastName", lastName);
        claims.put("specialization", specialization);
        claims.put("verificationStatus", verificationStatus);
        claims.put("role", "healthcare_provider");
        claims.put("jti", UUID.randomUUID().toString()); // Unique token ID

        return createToken(claims, email, accessTokenExpiration);
    }

    public String generateRefreshToken(UUID uuid, String email, boolean rememberMe) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("uuid", uuid.toString());
        claims.put("email", email);
        claims.put("type", "refresh");
        claims.put("jti", UUID.randomUUID().toString()); // Unique token ID

        long expiration = rememberMe ? rememberMeExpiration : refreshTokenExpiration;
        return createToken(claims, email, expiration);
    }

    private String createToken(Map<String, Object> claims, String subject, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    public Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            log.error("Error parsing JWT token: {}", e.getMessage());
            throw e;
        }
    }

    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    public UUID extractUuid(String token) {
        String uuidStr = extractAllClaims(token).get("uuid", String.class);
        return UUID.fromString(uuidStr);
    }

    public String extractTokenId(String token) {
        return extractAllClaims(token).get("jti", String.class);
    }

    public String extractTokenType(String token) {
        return extractAllClaims(token).get("type", String.class);
    }

    public Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    public boolean isTokenExpired(String token) {
        try {
            Date expiration = extractExpiration(token);
            return expiration.before(new Date());
        } catch (Exception e) {
            log.error("Error checking token expiration: {}", e.getMessage());
            return true;
        }
    }

    public boolean isRefreshToken(String token) {
        try {
            String tokenType = extractTokenType(token);
            return "refresh".equals(tokenType);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean validateToken(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            log.error("Error validating token: {}", e.getMessage());
            return false;
        }
    }

    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    public long getRefreshTokenExpiration(boolean rememberMe) {
        return rememberMe ? rememberMeExpiration : refreshTokenExpiration;
    }
} 