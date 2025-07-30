package com.example.session_demo.controller;

import com.example.session_demo.dto.ApiResponse;
import com.example.session_demo.dto.EnhancedLoginRequestDTO;
import com.example.session_demo.dto.EnhancedLoginResponseDTO;
import com.example.session_demo.dto.TokenRefreshRequestDTO;
import com.example.session_demo.dto.TokenRefreshResponseDTO;
import com.example.session_demo.service.EnhancedAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/provider")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Enhanced Authentication", description = "Enterprise-grade authentication endpoints for healthcare providers")
public class EnhancedAuthController {

    private final EnhancedAuthService enhancedAuthService;

    @PostMapping("/login")
    @Operation(
        summary = "Enhanced provider login",
        description = "Authenticate a healthcare provider with comprehensive security features including brute force protection, rate limiting, and audit logging"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication failed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "423", description = "Account locked"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Rate limited"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Validation failed")
    })
    public ResponseEntity<ApiResponse<EnhancedLoginResponseDTO>> login(
            @Valid @RequestBody EnhancedLoginRequestDTO loginRequest,
            BindingResult bindingResult,
            HttpServletRequest request) {

        log.info("Enhanced login request received from IP: {}", getClientIpAddress(request));

        try {
            // Check for validation errors
            if (bindingResult.hasErrors()) {
                Map<String, List<String>> errors = new HashMap<>();
                for (FieldError error : bindingResult.getFieldErrors()) {
                    errors.computeIfAbsent(error.getField(), k -> new java.util.ArrayList<>())
                           .add(error.getDefaultMessage());
                }

                ApiResponse<EnhancedLoginResponseDTO> response = ApiResponse.<EnhancedLoginResponseDTO>builder()
                    .success(false)
                    .message("Validation failed")
                    .errorCode("VALIDATION_ERROR")
                    .errors(errors)
                    .build();

                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
            }

            EnhancedLoginResponseDTO loginResponse = enhancedAuthService.login(
                loginRequest, 
                getClientIpAddress(request), 
                request.getHeader("User-Agent")
            );

            ApiResponse<EnhancedLoginResponseDTO> response = ApiResponse.<EnhancedLoginResponseDTO>builder()
                .success(true)
                .message("Login successful")
                .data(loginResponse)
                .build();

            return ResponseEntity.ok(response);

        } catch (EnhancedAuthService.AuthenticationException e) {
            log.warn("Authentication failed: {}", e.getMessage());
            
            String errorCode = determineErrorCode(e.getMessage());
            HttpStatus status = determineHttpStatus(errorCode);
            
            ApiResponse<EnhancedLoginResponseDTO> response = ApiResponse.<EnhancedLoginResponseDTO>builder()
                .success(false)
                .message(e.getMessage())
                .errorCode(errorCode)
                .build();

            return ResponseEntity.status(status).body(response);

        } catch (Exception e) {
            log.error("Unexpected error during login: {}", e.getMessage(), e);
            
            ApiResponse<EnhancedLoginResponseDTO> response = ApiResponse.<EnhancedLoginResponseDTO>builder()
                .success(false)
                .message("Internal server error occurred")
                .errorCode("INTERNAL_ERROR")
                .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/refresh")
    @Operation(
        summary = "Refresh access token",
        description = "Refresh an expired access token using a valid refresh token"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid refresh token"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Validation failed")
    })
    public ResponseEntity<ApiResponse<TokenRefreshResponseDTO>> refreshToken(
            @Valid @RequestBody TokenRefreshRequestDTO refreshRequest,
            BindingResult bindingResult,
            HttpServletRequest request) {

        log.info("Token refresh request received from IP: {}", getClientIpAddress(request));

        try {
            // Check for validation errors
            if (bindingResult.hasErrors()) {
                Map<String, List<String>> errors = new HashMap<>();
                for (FieldError error : bindingResult.getFieldErrors()) {
                    errors.computeIfAbsent(error.getField(), k -> new java.util.ArrayList<>())
                           .add(error.getDefaultMessage());
                }

                ApiResponse<TokenRefreshResponseDTO> response = ApiResponse.<TokenRefreshResponseDTO>builder()
                    .success(false)
                    .message("Validation failed")
                    .errorCode("VALIDATION_ERROR")
                    .errors(errors)
                    .build();

                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
            }

            TokenRefreshResponseDTO refreshResponse = enhancedAuthService.refreshToken(
                refreshRequest, 
                getClientIpAddress(request)
            );

            ApiResponse<TokenRefreshResponseDTO> response = ApiResponse.<TokenRefreshResponseDTO>builder()
                .success(true)
                .message("Token refreshed successfully")
                .data(refreshResponse)
                .build();

            return ResponseEntity.ok(response);

        } catch (EnhancedAuthService.AuthenticationException e) {
            log.warn("Token refresh failed: {}", e.getMessage());
            
            ApiResponse<TokenRefreshResponseDTO> response = ApiResponse.<TokenRefreshResponseDTO>builder()
                .success(false)
                .message(e.getMessage())
                .errorCode("AUTHENTICATION_FAILED")
                .build();

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);

        } catch (Exception e) {
            log.error("Unexpected error during token refresh: {}", e.getMessage(), e);
            
            ApiResponse<TokenRefreshResponseDTO> response = ApiResponse.<TokenRefreshResponseDTO>builder()
                .success(false)
                .message("Internal server error occurred")
                .errorCode("INTERNAL_ERROR")
                .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/logout")
    @Operation(
        summary = "Logout from current session",
        description = "Logout from the current session by revoking the refresh token"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Logout successful"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Validation failed")
    })
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestParam String refreshToken,
            HttpServletRequest request) {

        log.info("Logout request received from IP: {}", getClientIpAddress(request));

        try {
            enhancedAuthService.logout(refreshToken, getClientIpAddress(request));

            ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(true)
                .message("Logout successful")
                .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error during logout: {}", e.getMessage(), e);
            
            ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(false)
                .message("Logout failed")
                .errorCode("LOGOUT_FAILED")
                .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/logout-all")
    @Operation(
        summary = "Logout from all sessions",
        description = "Logout from all active sessions for the authenticated provider"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Logout from all sessions successful"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<Void>> logoutAll(
            @RequestParam String providerUuid,
            HttpServletRequest request) {

        log.info("Logout all sessions request received for provider: {} from IP: {}", providerUuid, getClientIpAddress(request));

        try {
            enhancedAuthService.logoutAll(java.util.UUID.fromString(providerUuid));

            ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(true)
                .message("Logout from all sessions successful")
                .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error during logout all: {}", e.getMessage(), e);
            
            ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(false)
                .message("Logout from all sessions failed")
                .errorCode("LOGOUT_ALL_FAILED")
                .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Helper methods

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    private String determineErrorCode(String message) {
        if (message.contains("locked")) {
            return "ACCOUNT_LOCKED";
        } else if (message.contains("Too many login attempts")) {
            return "RATE_LIMITED";
        } else if (message.contains("not verified")) {
            return "EMAIL_NOT_VERIFIED";
        } else if (message.contains("deactivated")) {
            return "ACCOUNT_DISABLED";
        } else {
            return "AUTHENTICATION_FAILED";
        }
    }

    private HttpStatus determineHttpStatus(String errorCode) {
        return switch (errorCode) {
            case "ACCOUNT_LOCKED" -> HttpStatus.LOCKED; // 423
            case "RATE_LIMITED" -> HttpStatus.TOO_MANY_REQUESTS; // 429
            case "EMAIL_NOT_VERIFIED" -> HttpStatus.FORBIDDEN; // 403
            case "ACCOUNT_DISABLED" -> HttpStatus.FORBIDDEN; // 403
            default -> HttpStatus.UNAUTHORIZED; // 401
        };
    }
} 