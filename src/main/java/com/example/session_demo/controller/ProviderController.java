package com.example.session_demo.controller;

import com.example.session_demo.dto.ApiResponse;
import com.example.session_demo.dto.ProviderRegisterDTO;
import com.example.session_demo.dto.ProviderResponseDTO;
import com.example.session_demo.dto.ProviderUpdateDTO;
import com.example.session_demo.enums.VerificationStatus;
import com.example.session_demo.service.ProviderService;
import com.example.session_demo.service.RateLimitingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;

@RestController
@RequestMapping("/api/v1/provider")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Provider Management", description = "Healthcare provider registration and management")
public class ProviderController {

    private final ProviderService providerService;
    private final RateLimitingService rateLimitingService;

    @PostMapping("/register")
    @Operation(
        summary = "Register a new healthcare provider",
        description = "Register a new healthcare provider with email verification. Rate limited to 5 attempts per hour per IP."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Provider registered successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Provider already exists"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Validation failed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Too many requests")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> registerProvider(
            @Valid @RequestBody ProviderRegisterDTO registerDTO,
            BindingResult bindingResult,
            HttpServletRequest request) {

        String ipAddress = getClientIpAddress(request);
        log.info("Provider registration attempt from IP: {}", ipAddress);

        try {
            // Check rate limiting
            if (rateLimitingService.isRateLimited(ipAddress)) {
                int remainingAttempts = rateLimitingService.getRemainingAttempts(ipAddress);
                long timeUntilReset = rateLimitingService.getTimeUntilReset(ipAddress);
                
                Map<String, Object> errorData = new HashMap<>();
                errorData.put("remainingAttempts", remainingAttempts);
                errorData.put("timeUntilResetMs", timeUntilReset);
                errorData.put("message", "Too many registration attempts. Please try again later.");

                ApiResponse<Map<String, Object>> response = ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Rate limit exceeded")
                    .errorCode("RATE_LIMIT_EXCEEDED")
                    .data(errorData)
                    .build();

                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
            }

            // Increment rate limit counter
            rateLimitingService.incrementRequestCount(ipAddress);

            // Check for validation errors
            if (bindingResult.hasErrors()) {
                Map<String, List<String>> errors = new HashMap<>();
                for (FieldError error : bindingResult.getFieldErrors()) {
                    errors.computeIfAbsent(error.getField(), k -> new java.util.ArrayList<>())
                           .add(error.getDefaultMessage());
                }

                ApiResponse<Map<String, Object>> response = ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Validation failed")
                    .errorCode("VALIDATION_ERROR")
                    .errors(errors)
                    .build();

                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
            }

            // Register the provider
            ProviderResponseDTO providerResponse = providerService.registerProvider(registerDTO, ipAddress);

            // Prepare success response data
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("provider_id", providerResponse.getUuid());
            responseData.put("email", providerResponse.getEmail());
            responseData.put("verification_status", providerResponse.getVerificationStatus());
            responseData.put("created_at", providerResponse.getCreatedAt());

            ApiResponse<Map<String, Object>> response = ApiResponse.<Map<String, Object>>builder()
                .success(true)
                .message("Provider registered successfully. Verification email sent.")
                .data(responseData)
                .build();

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (ProviderService.ValidationException e) {
            log.warn("Validation error during provider registration: {}", e.getMessage());
            
            ApiResponse<Map<String, Object>> response = ApiResponse.<Map<String, Object>>builder()
                .success(false)
                .message("Validation failed")
                .errorCode("VALIDATION_ERROR")
                .errors(e.getErrors())
                .build();

            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);

        } catch (ProviderService.ConflictException e) {
            log.warn("Conflict during provider registration: {}", e.getMessage());
            
            ApiResponse<Map<String, Object>> response = ApiResponse.<Map<String, Object>>builder()
                .success(false)
                .message(e.getMessage())
                .errorCode("CONFLICT_ERROR")
                .build();

            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);

        } catch (Exception e) {
            log.error("Unexpected error during provider registration: {}", e.getMessage(), e);
            
            ApiResponse<Map<String, Object>> response = ApiResponse.<Map<String, Object>>builder()
                .success(false)
                .message("Internal server error occurred")
                .errorCode("INTERNAL_ERROR")
                .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/verify-email")
    @Operation(
        summary = "Verify provider email address",
        description = "Verify provider email address using the verification token sent via email"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyEmail(
            @Parameter(description = "Email verification token") @RequestParam String token) {

        log.info("Email verification attempt with token: {}", token.substring(0, Math.min(8, token.length())) + "...");

        try {
            boolean verified = providerService.verifyEmail(token);

            if (verified) {
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("verified", true);
                responseData.put("message", "Email verified successfully");
                responseData.put("verified_at", LocalDateTime.now());

                ApiResponse<Map<String, Object>> response = ApiResponse.<Map<String, Object>>builder()
                    .success(true)
                    .message("Email verified successfully")
                    .data(responseData)
                    .build();

                return ResponseEntity.ok(response);

            } else {
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("verified", false);
                responseData.put("message", "Invalid or expired verification token");

                ApiResponse<Map<String, Object>> response = ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Email verification failed")
                    .errorCode("VERIFICATION_FAILED")
                    .data(responseData)
                    .build();

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

        } catch (Exception e) {
            log.error("Error during email verification: {}", e.getMessage(), e);
            
            ApiResponse<Map<String, Object>> response = ApiResponse.<Map<String, Object>>builder()
                .success(false)
                .message("Email verification failed due to internal error")
                .errorCode("INTERNAL_ERROR")
                .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/rate-limit-status")
    @Operation(
        summary = "Check rate limit status",
        description = "Check the current rate limit status for the requesting IP address"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRateLimitStatus(HttpServletRequest request) {
        String ipAddress = getClientIpAddress(request);
        
        Map<String, Object> statusData = new HashMap<>();
        statusData.put("ipAddress", ipAddress);
        statusData.put("isRateLimited", rateLimitingService.isRateLimited(ipAddress));
        statusData.put("remainingAttempts", rateLimitingService.getRemainingAttempts(ipAddress));
        statusData.put("currentCount", rateLimitingService.getCurrentRequestCount(ipAddress));
        statusData.put("timeUntilResetMs", rateLimitingService.getTimeUntilReset(ipAddress));

        ApiResponse<Map<String, Object>> response = ApiResponse.<Map<String, Object>>builder()
            .success(true)
            .message("Rate limit status retrieved successfully")
            .data(statusData)
            .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping
    @SecurityRequirement(name = "OAuth2Auth")
    @Operation(
        summary = "Get all providers with pagination",
        description = "Retrieve all healthcare providers with pagination support"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllProviders(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<ProviderResponseDTO> providersPage = providerService.getAllProviders(pageable);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("providers", providersPage.getContent());
            responseData.put("currentPage", providersPage.getNumber());
            responseData.put("totalPages", providersPage.getTotalPages());
            responseData.put("totalElements", providersPage.getTotalElements());
            responseData.put("pageSize", providersPage.getSize());

            ApiResponse<Map<String, Object>> response = ApiResponse.<Map<String, Object>>builder()
                .success(true)
                .message("Providers retrieved successfully")
                .data(responseData)
                .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error retrieving providers: {}", e.getMessage(), e);
            
            ApiResponse<Map<String, Object>> response = ApiResponse.<Map<String, Object>>builder()
                .success(false)
                .message("Failed to retrieve providers")
                .errorCode("INTERNAL_ERROR")
                .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/{id}")
    @SecurityRequirement(name = "OAuth2Auth")
    @Operation(
        summary = "Get provider by ID",
        description = "Retrieve a specific healthcare provider by their ID"
    )
    public ResponseEntity<ApiResponse<ProviderResponseDTO>> getProviderById(
            @Parameter(description = "Provider ID") @PathVariable UUID id) {

        try {
            Optional<ProviderResponseDTO> providerOpt = providerService.getProviderById(id);
            
            if (providerOpt.isPresent()) {
                ApiResponse<ProviderResponseDTO> response = ApiResponse.<ProviderResponseDTO>builder()
                    .success(true)
                    .message("Provider retrieved successfully")
                    .data(providerOpt.get())
                    .build();
                return ResponseEntity.ok(response);
            } else {
                ApiResponse<ProviderResponseDTO> response = ApiResponse.<ProviderResponseDTO>builder()
                    .success(false)
                    .message("Provider not found")
                    .errorCode("NOT_FOUND")
                    .build();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

        } catch (Exception e) {
            log.error("Error retrieving provider {}: {}", id, e.getMessage(), e);
            
            ApiResponse<ProviderResponseDTO> response = ApiResponse.<ProviderResponseDTO>builder()
                .success(false)
                .message("Failed to retrieve provider")
                .errorCode("INTERNAL_ERROR")
                .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PutMapping("/{id}")
    @SecurityRequirement(name = "OAuth2Auth")
    @Operation(
        summary = "Update provider information",
        description = "Update healthcare provider information by ID"
    )
    public ResponseEntity<ApiResponse<ProviderResponseDTO>> updateProvider(
            @Parameter(description = "Provider ID") @PathVariable UUID id,
            @Valid @RequestBody ProviderUpdateDTO updateDTO,
            BindingResult bindingResult) {

        try {
            // Check for validation errors
            if (bindingResult.hasErrors()) {
                Map<String, List<String>> errors = new HashMap<>();
                for (FieldError error : bindingResult.getFieldErrors()) {
                    errors.computeIfAbsent(error.getField(), k -> new java.util.ArrayList<>())
                           .add(error.getDefaultMessage());
                }

                ApiResponse<ProviderResponseDTO> response = ApiResponse.<ProviderResponseDTO>builder()
                    .success(false)
                    .message("Validation failed")
                    .errorCode("VALIDATION_ERROR")
                    .errors(errors)
                    .build();

                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
            }

            ProviderResponseDTO updatedProvider = providerService.updateProvider(id, updateDTO);

            ApiResponse<ProviderResponseDTO> response = ApiResponse.<ProviderResponseDTO>builder()
                .success(true)
                .message("Provider updated successfully")
                .data(updatedProvider)
                .build();

            return ResponseEntity.ok(response);

        } catch (ProviderService.NotFoundException e) {
            ApiResponse<ProviderResponseDTO> response = ApiResponse.<ProviderResponseDTO>builder()
                .success(false)
                .message(e.getMessage())
                .errorCode("NOT_FOUND")
                .build();

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (ProviderService.ValidationException e) {
            ApiResponse<ProviderResponseDTO> response = ApiResponse.<ProviderResponseDTO>builder()
                .success(false)
                .message("Validation failed")
                .errorCode("VALIDATION_ERROR")
                .errors(e.getErrors())
                .build();

            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);

        } catch (ProviderService.ConflictException e) {
            ApiResponse<ProviderResponseDTO> response = ApiResponse.<ProviderResponseDTO>builder()
                .success(false)
                .message(e.getMessage())
                .errorCode("CONFLICT_ERROR")
                .build();

            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);

        } catch (Exception e) {
            log.error("Error updating provider {}: {}", id, e.getMessage(), e);
            
            ApiResponse<ProviderResponseDTO> response = ApiResponse.<ProviderResponseDTO>builder()
                .success(false)
                .message("Failed to update provider")
                .errorCode("INTERNAL_ERROR")
                .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "OAuth2Auth")
    @Operation(
        summary = "Delete provider (soft delete)",
        description = "Soft delete a healthcare provider by marking them as inactive"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteProvider(
            @Parameter(description = "Provider ID") @PathVariable UUID id) {

        try {
            providerService.deleteProvider(id);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("providerId", id);
            responseData.put("deletedAt", LocalDateTime.now());

            ApiResponse<Map<String, Object>> response = ApiResponse.<Map<String, Object>>builder()
                .success(true)
                .message("Provider deleted successfully")
                .data(responseData)
                .build();

            return ResponseEntity.ok(response);

        } catch (ProviderService.NotFoundException e) {
            ApiResponse<Map<String, Object>> response = ApiResponse.<Map<String, Object>>builder()
                .success(false)
                .message(e.getMessage())
                .errorCode("NOT_FOUND")
                .build();

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            log.error("Error deleting provider {}: {}", id, e.getMessage(), e);
            
            ApiResponse<Map<String, Object>> response = ApiResponse.<Map<String, Object>>builder()
                .success(false)
                .message("Failed to delete provider")
                .errorCode("INTERNAL_ERROR")
                .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/search")
    @SecurityRequirement(name = "OAuth2Auth")
    @Operation(
        summary = "Search providers",
        description = "Search healthcare providers by name or email"
    )
    public ResponseEntity<ApiResponse<List<ProviderResponseDTO>>> searchProviders(
            @Parameter(description = "Search term") @RequestParam String q) {

        try {
            List<ProviderResponseDTO> providers = providerService.searchProviders(q);

            ApiResponse<List<ProviderResponseDTO>> response = ApiResponse.<List<ProviderResponseDTO>>builder()
                .success(true)
                .message("Search completed successfully")
                .data(providers)
                .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error searching providers with term '{}': {}", q, e.getMessage(), e);
            
            ApiResponse<List<ProviderResponseDTO>> response = ApiResponse.<List<ProviderResponseDTO>>builder()
                .success(false)
                .message("Search failed")
                .errorCode("INTERNAL_ERROR")
                .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PutMapping("/{id}/status")
    @SecurityRequirement(name = "OAuth2Auth")
    @Operation(
        summary = "Update provider status",
        description = "Activate or deactivate a healthcare provider"
    )
    public ResponseEntity<ApiResponse<ProviderResponseDTO>> updateProviderStatus(
            @Parameter(description = "Provider ID") @PathVariable UUID id,
            @Parameter(description = "Active status") @RequestParam boolean isActive) {

        try {
            ProviderResponseDTO provider = providerService.updateProviderStatus(id, isActive);

            ApiResponse<ProviderResponseDTO> response = ApiResponse.<ProviderResponseDTO>builder()
                .success(true)
                .message("Provider status updated successfully")
                .data(provider)
                .build();

            return ResponseEntity.ok(response);

        } catch (ProviderService.NotFoundException e) {
            ApiResponse<ProviderResponseDTO> response = ApiResponse.<ProviderResponseDTO>builder()
                .success(false)
                .message(e.getMessage())
                .errorCode("NOT_FOUND")
                .build();

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            log.error("Error updating provider status {}: {}", id, e.getMessage(), e);
            
            ApiResponse<ProviderResponseDTO> response = ApiResponse.<ProviderResponseDTO>builder()
                .success(false)
                .message("Failed to update provider status")
                .errorCode("INTERNAL_ERROR")
                .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PutMapping("/{id}/verification-status")
    @SecurityRequirement(name = "OAuth2Auth")
    @Operation(
        summary = "Update provider verification status",
        description = "Update the verification status of a healthcare provider"
    )
    public ResponseEntity<ApiResponse<ProviderResponseDTO>> updateVerificationStatus(
            @Parameter(description = "Provider ID") @PathVariable UUID id,
            @Parameter(description = "Verification status") @RequestParam VerificationStatus status) {

        try {
            ProviderResponseDTO provider = providerService.updateVerificationStatus(id, status);

            ApiResponse<ProviderResponseDTO> response = ApiResponse.<ProviderResponseDTO>builder()
                .success(true)
                .message("Provider verification status updated successfully")
                .data(provider)
                .build();

            return ResponseEntity.ok(response);

        } catch (ProviderService.NotFoundException e) {
            ApiResponse<ProviderResponseDTO> response = ApiResponse.<ProviderResponseDTO>builder()
                .success(false)
                .message(e.getMessage())
                .errorCode("NOT_FOUND")
                .build();

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            log.error("Error updating provider verification status {}: {}", id, e.getMessage(), e);
            
            ApiResponse<ProviderResponseDTO> response = ApiResponse.<ProviderResponseDTO>builder()
                .success(false)
                .message("Failed to update provider verification status")
                .errorCode("INTERNAL_ERROR")
                .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Extract client IP address from request, considering proxy headers
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };

        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // Handle multiple IPs in X-Forwarded-For header
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        return request.getRemoteAddr();
    }
} 