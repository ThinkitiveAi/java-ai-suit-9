package com.example.session_demo.service;

import com.example.session_demo.dto.LoginRequestDTO;
import com.example.session_demo.dto.LoginResponseDTO;
import com.example.session_demo.entity.Provider;
import com.example.session_demo.repository.ProviderRepository;
import com.example.session_demo.util.JwtUtil;
import com.example.session_demo.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final ProviderRepository providerRepository;
    private final JwtUtil jwtUtil;
    private final PasswordUtil passwordUtil;

    public LoginResponseDTO login(LoginRequestDTO loginRequest) {
        log.info("Login attempt for email: {}", loginRequest.getEmail());

        // Find provider by email
        Optional<Provider> providerOpt = providerRepository.findByEmail(loginRequest.getEmail());
        
        if (providerOpt.isEmpty()) {
            log.warn("Login failed: Provider not found for email: {}", loginRequest.getEmail());
            throw new AuthenticationException("Invalid email or password");
        }

        Provider provider = providerOpt.get();

        // Check if provider is active
        if (!provider.getIsActive()) {
            log.warn("Login failed: Inactive provider for email: {}", loginRequest.getEmail());
            throw new AuthenticationException("Account is deactivated. Please contact support.");
        }

        // Check if email is verified
        if (!provider.getEmailVerified()) {
            log.warn("Login failed: Email not verified for email: {}", loginRequest.getEmail());
            throw new AuthenticationException("Email not verified. Please verify your email before logging in.");
        }

        // Verify password
        if (!passwordUtil.verifyPassword(loginRequest.getPassword(), provider.getPasswordHash())) {
            log.warn("Login failed: Invalid password for email: {}", loginRequest.getEmail());
            throw new AuthenticationException("Invalid email or password");
        }

        // Generate JWT token
        String token = jwtUtil.generateToken(
            provider.getUuid(), 
            provider.getEmail(), 
            provider.getFirstName(), 
            provider.getLastName()
        );

        log.info("Login successful for provider: {}", provider.getId());

        return LoginResponseDTO.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getExpirationTime())
                .id(provider.getId())
                .uuid(provider.getUuid())
                .email(provider.getEmail())
                .firstName(provider.getFirstName())
                .lastName(provider.getLastName())
                .loginTime(LocalDateTime.now())
                .build();
    }

    public static class AuthenticationException extends RuntimeException {
        public AuthenticationException(String message) {
            super(message);
        }
    }
} 