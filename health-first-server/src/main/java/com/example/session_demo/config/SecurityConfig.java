package com.example.session_demo.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Enable CORS with custom configuration
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(authz -> authz
                // Public endpoints - provider registration and verification
                .requestMatchers("/api/v1/provider/register").permitAll()
                .requestMatchers("/api/v1/provider/login").permitAll()
                .requestMatchers("/api/v1/provider/verify-email").permitAll()
                .requestMatchers("/api/v1/provider/rate-limit-status").permitAll()
                
                // Authentication endpoints
                .requestMatchers("/api/v1/auth/login").permitAll()

                // Patient public endpoints
                .requestMatchers("/api/v1/patient/register").permitAll()
                .requestMatchers("/api/v1/patient/verify-email").permitAll()
                .requestMatchers("/api/v1/patient/verify-phone").permitAll()
                .requestMatchers("/api/v1/patient/resend-verification").permitAll()
                .requestMatchers("/api/v1/patient/check-email").permitAll()
                .requestMatchers("/api/v1/patient/check-phone").permitAll()

                
                // Swagger/OpenAPI endpoints
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/swagger-ui.html").permitAll()
                .requestMatchers("/v3/api-docs/**").permitAll()
                .requestMatchers("/api-docs/**").permitAll()
                
                // Health check endpoints
                .requestMatchers("/actuator/health").permitAll()
                
                // All other endpoints require authentication
                .anyRequest().authenticated()
            );

        return http.build();
    }
} 