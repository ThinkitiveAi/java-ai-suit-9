package com.example.session_demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitingService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${rate.limit.window.ms:3600000}") // 1 hour default
    private long windowSizeMs;

    @Value("${rate.limit.max.requests:5}") // 5 requests default
    private int maxRequests;

    private static final String RATE_LIMIT_PREFIX = "rate_limit:registration:";

    /**
     * Check if the IP address has exceeded the rate limit
     */
    public boolean isRateLimited(String ipAddress) {
        try {
            String key = RATE_LIMIT_PREFIX + ipAddress;
            String countStr = (String) redisTemplate.opsForValue().get(key);
            
            if (countStr == null) {
                return false; // No previous requests
            }
            
            int currentCount = Integer.parseInt(countStr);
            boolean isLimited = currentCount >= maxRequests;
            
            if (isLimited) {
                log.warn("Rate limit exceeded for IP: {}. Current count: {}", ipAddress, currentCount);
            }
            
            return isLimited;
            
        } catch (Exception e) {
            log.error("Error checking rate limit for IP {}: {}", ipAddress, e.getMessage());
            return false; // Allow request if Redis is unavailable
        }
    }

    /**
     * Increment the request count for an IP address
     */
    public void incrementRequestCount(String ipAddress) {
        try {
            String key = RATE_LIMIT_PREFIX + ipAddress;
            String countStr = (String) redisTemplate.opsForValue().get(key);
            
            if (countStr == null) {
                // First request from this IP
                redisTemplate.opsForValue().set(key, "1", Duration.ofMillis(windowSizeMs));
                log.debug("First registration attempt from IP: {}", ipAddress);
            } else {
                // Increment existing count
                int currentCount = Integer.parseInt(countStr);
                redisTemplate.opsForValue().set(key, String.valueOf(currentCount + 1), Duration.ofMillis(windowSizeMs));
                log.debug("Registration attempt {} from IP: {}", currentCount + 1, ipAddress);
            }
            
        } catch (Exception e) {
            log.error("Error incrementing request count for IP {}: {}", ipAddress, e.getMessage());
        }
    }

    /**
     * Get remaining attempts for an IP address
     */
    public int getRemainingAttempts(String ipAddress) {
        try {
            String key = RATE_LIMIT_PREFIX + ipAddress;
            String countStr = (String) redisTemplate.opsForValue().get(key);
            
            if (countStr == null) {
                return maxRequests;
            }
            
            int currentCount = Integer.parseInt(countStr);
            return Math.max(0, maxRequests - currentCount);
            
        } catch (Exception e) {
            log.error("Error getting remaining attempts for IP {}: {}", ipAddress, e.getMessage());
            return maxRequests; // Return max if Redis is unavailable
        }
    }

    /**
     * Get time until rate limit reset for an IP address
     */
    public long getTimeUntilReset(String ipAddress) {
        try {
            String key = RATE_LIMIT_PREFIX + ipAddress;
            Long ttl = redisTemplate.getExpire(key, TimeUnit.MILLISECONDS);
            return ttl != null && ttl > 0 ? ttl : 0;
            
        } catch (Exception e) {
            log.error("Error getting TTL for IP {}: {}", ipAddress, e.getMessage());
            return 0;
        }
    }

    /**
     * Reset rate limit for an IP address (admin function)
     */
    public void resetRateLimit(String ipAddress) {
        try {
            String key = RATE_LIMIT_PREFIX + ipAddress;
            redisTemplate.delete(key);
            log.info("Rate limit reset for IP: {}", ipAddress);
            
        } catch (Exception e) {
            log.error("Error resetting rate limit for IP {}: {}", ipAddress, e.getMessage());
        }
    }

    /**
     * Get current request count for an IP address
     */
    public int getCurrentRequestCount(String ipAddress) {
        try {
            String key = RATE_LIMIT_PREFIX + ipAddress;
            String countStr = (String) redisTemplate.opsForValue().get(key);
            return countStr != null ? Integer.parseInt(countStr) : 0;
            
        } catch (Exception e) {
            log.error("Error getting current request count for IP {}: {}", ipAddress, e.getMessage());
            return 0;
        }
    }
} 