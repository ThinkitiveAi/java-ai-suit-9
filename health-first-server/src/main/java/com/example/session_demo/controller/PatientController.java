package com.example.session_demo.controller;

import com.example.session_demo.dto.ApiResponse;
import com.example.session_demo.dto.PatientRegistrationRequestDTO;
import com.example.session_demo.dto.PatientRegistrationResponseDTO;
import com.example.session_demo.dto.VerificationRequestDTO;
import com.example.session_demo.dto.VerificationResponseDTO;
import com.example.session_demo.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@RequestMapping("/api/v1/patient")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Patient Management", description = "HIPAA-compliant patient registration and verification endpoints")
public class PatientController {

    private final PatientService patientService;

    @PostMapping("/register")
    @Operation(
        summary = "Register a new patient",
        description = "Register a new patient with comprehensive HIPAA-compliant validation, COPPA age verification, and dual verification (email + SMS) workflow"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Patient registered successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Age restriction or invalid input"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Email or phone number already exists"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Validation failed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Rate limited")
    })
    public ResponseEntity<ApiResponse<PatientRegistrationResponseDTO>> registerPatient(
            @Valid @RequestBody PatientRegistrationRequestDTO registrationRequest,
            BindingResult bindingResult,
            HttpServletRequest request) {
        
        try {
            log.info("Patient registration attempt from IP: {}", getClientIpAddress(request));
            
            // Handle validation errors
            if (bindingResult.hasErrors()) {
                Map<String, List<String>> validationErrors = new HashMap<>();
                for (FieldError error : bindingResult.getFieldErrors()) {
                    validationErrors.computeIfAbsent(error.getField(), k -> new java.util.ArrayList<>())
                                  .add(error.getDefaultMessage());
                }
                
                ApiResponse<PatientRegistrationResponseDTO> response = ApiResponse.<PatientRegistrationResponseDTO>builder()
                    .success(false)
                    .message("Validation failed")
                    .errorCode("VALIDATION_ERROR")
                    .errors(validationErrors)
                    .build();
                    
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
            }
            
            // Check for password confirmation match
            if (!registrationRequest.isPasswordMatch()) {
                Map<String, List<String>> passwordErrors = new HashMap<>();
                passwordErrors.put("confirm_password", List.of("Password confirmation does not match"));
                
                ApiResponse<PatientRegistrationResponseDTO> response = ApiResponse.<PatientRegistrationResponseDTO>builder()
                    .success(false)
                    .message("Password confirmation does not match")
                    .errorCode("PASSWORD_MISMATCH")
                    .errors(passwordErrors)
                    .build();
                    
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
            }
            
            // Check minimum age requirement (COPPA compliance)
            if (!registrationRequest.isValidAge(13)) {
                ApiResponse<PatientRegistrationResponseDTO> response = ApiResponse.<PatientRegistrationResponseDTO>builder()
                    .success(false)
                    .message("Registration requires minimum age of 13 years")
                    .errorCode("AGE_RESTRICTION")
                    .build();
                    
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            // Check for existing email
            if (patientService.existsByEmail(registrationRequest.getEmail())) {
                ApiResponse<PatientRegistrationResponseDTO> response = ApiResponse.<PatientRegistrationResponseDTO>builder()
                    .success(false)
                    .message("An account with this email address already exists")
                    .errorCode("DUPLICATE_EMAIL")
                    .build();
                    
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }
            
            // Check for existing phone number
            if (patientService.existsByPhoneNumber(registrationRequest.getPhoneNumber())) {
                ApiResponse<PatientRegistrationResponseDTO> response = ApiResponse.<PatientRegistrationResponseDTO>builder()
                    .success(false)
                    .message("An account with this phone number already exists")
                    .errorCode("DUPLICATE_PHONE")
                    .build();
                    
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }
            
            // Sanitize input data
            registrationRequest.sanitizeData();
            
            // Register the patient
            PatientRegistrationResponseDTO responseData = patientService.registerPatient(
                registrationRequest, 
                getClientIpAddress(request), 
                request.getHeader("User-Agent")
            );
            
            ApiResponse<PatientRegistrationResponseDTO> response = ApiResponse.success(
                "Patient registered successfully. Verification email and SMS sent.", 
                responseData
            );
            
            log.info("Patient registration successful for email: {}", registrationRequest.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            log.error("Patient registration failed", e);
            ApiResponse<PatientRegistrationResponseDTO> response = ApiResponse.error(
                "Registration failed due to internal error", 
                "INTERNAL_ERROR"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/verify-email")
    @Operation(
        summary = "Verify patient email",
        description = "Verify patient email address using the verification token sent via email"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Email verified successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid or expired token"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Token not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Too many attempts")
    })
    public ResponseEntity<ApiResponse<VerificationResponseDTO>> verifyEmail(
            @Parameter(description = "Email verification token", required = true)
            @RequestParam("token") String token,
            HttpServletRequest request) {
        
        try {
            log.info("Email verification attempt from IP: {}", getClientIpAddress(request));
            
            VerificationResponseDTO responseData = patientService.verifyEmail(
                token, 
                getClientIpAddress(request)
            );
            
            ApiResponse<VerificationResponseDTO> response = ApiResponse.success(
                "Email verification completed successfully", 
                responseData
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Email verification failed", e);
            ApiResponse<VerificationResponseDTO> response = ApiResponse.error(
                "Email verification failed", 
                "VERIFICATION_FAILED"
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @PostMapping("/verify-phone")
    @Operation(
        summary = "Verify patient phone number",
        description = "Verify patient phone number using the OTP code sent via SMS"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Phone verified successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid or expired OTP"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Validation failed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Too many attempts")
    })
    public ResponseEntity<ApiResponse<VerificationResponseDTO>> verifyPhone(
            @Valid @RequestBody VerificationRequestDTO verificationRequest,
            BindingResult bindingResult,
            HttpServletRequest request) {
        
        try {
            log.info("Phone verification attempt from IP: {}", getClientIpAddress(request));
            
            // Handle validation errors
            if (bindingResult.hasErrors()) {
                Map<String, List<String>> validationErrors = new HashMap<>();
                for (FieldError error : bindingResult.getFieldErrors()) {
                    validationErrors.computeIfAbsent(error.getField(), k -> new java.util.ArrayList<>())
                                  .add(error.getDefaultMessage());
                }
                
                ApiResponse<VerificationResponseDTO> response = ApiResponse.<VerificationResponseDTO>builder()
                    .success(false)
                    .message("Validation failed")
                    .errorCode("VALIDATION_ERROR")
                    .errors(validationErrors)
                    .build();
                    
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
            }
            
            VerificationResponseDTO responseData = patientService.verifyPhone(
                verificationRequest, 
                getClientIpAddress(request)
            );
            
            ApiResponse<VerificationResponseDTO> response = ApiResponse.success(
                "Phone verification completed successfully", 
                responseData
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Phone verification failed", e);
            ApiResponse<VerificationResponseDTO> response = ApiResponse.error(
                "Phone verification failed", 
                "VERIFICATION_FAILED"
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @PostMapping("/resend-verification")
    @Operation(
        summary = "Resend verification",
        description = "Resend email or SMS verification for a patient with rate limiting"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Verification resent successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Validation failed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Rate limited")
    })
    public ResponseEntity<ApiResponse<VerificationResponseDTO>> resendVerification(
            @Valid @RequestBody VerificationRequestDTO verificationRequest,
            BindingResult bindingResult,
            HttpServletRequest request) {
        
        try {
            log.info("Resend verification attempt from IP: {} for type: {}", 
                    getClientIpAddress(request), verificationRequest.getVerificationType());
            
            // Handle validation errors
            if (bindingResult.hasErrors()) {
                Map<String, List<String>> validationErrors = new HashMap<>();
                for (FieldError error : bindingResult.getFieldErrors()) {
                    validationErrors.computeIfAbsent(error.getField(), k -> new java.util.ArrayList<>())
                                  .add(error.getDefaultMessage());
                }
                
                ApiResponse<VerificationResponseDTO> response = ApiResponse.<VerificationResponseDTO>builder()
                    .success(false)
                    .message("Validation failed")
                    .errorCode("VALIDATION_ERROR")
                    .errors(validationErrors)
                    .build();
                    
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
            }
            
            VerificationResponseDTO responseData = patientService.resendVerification(
                verificationRequest, 
                getClientIpAddress(request)
            );
            
            ApiResponse<VerificationResponseDTO> response = ApiResponse.success(
                "Verification resent successfully", 
                responseData
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Resend verification failed", e);
            ApiResponse<VerificationResponseDTO> response = ApiResponse.error(
                "Failed to resend verification", 
                "RESEND_FAILED"
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @GetMapping("/check-email")
    @Operation(
        summary = "Check if email exists",
        description = "Check if an email address is already registered (for client-side validation)"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Check completed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid email format")
    })
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkEmailExists(
            @Parameter(description = "Email address to check", required = true)
            @RequestParam("email") String email) {
        
        try {
            // Basic email validation
            if (email == null || email.trim().isEmpty() || !email.contains("@")) {
                ApiResponse<Map<String, Boolean>> response = ApiResponse.error(
                    "Invalid email format", 
                    "INVALID_EMAIL"
                );
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            boolean exists = patientService.existsByEmail(email.toLowerCase().trim());
            Map<String, Boolean> result = Map.of("exists", exists);
            
            ApiResponse<Map<String, Boolean>> response = ApiResponse.success(
                "Email check completed", 
                result
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Email check failed", e);
            ApiResponse<Map<String, Boolean>> response = ApiResponse.error(
                "Email check failed", 
                "CHECK_FAILED"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/check-phone")
    @Operation(
        summary = "Check if phone number exists",
        description = "Check if a phone number is already registered (for client-side validation)"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Check completed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid phone format")
    })
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkPhoneExists(
            @Parameter(description = "Phone number to check", required = true)
            @RequestParam("phone") String phone) {
        
        try {
            // Basic phone validation
            if (phone == null || phone.trim().isEmpty()) {
                ApiResponse<Map<String, Boolean>> response = ApiResponse.error(
                    "Invalid phone format", 
                    "INVALID_PHONE"
                );
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            boolean exists = patientService.existsByPhoneNumber(phone.trim());
            Map<String, Boolean> result = Map.of("exists", exists);
            
            ApiResponse<Map<String, Boolean>> response = ApiResponse.success(
                "Phone check completed", 
                result
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Phone check failed", e);
            ApiResponse<Map<String, Boolean>> response = ApiResponse.error(
                "Phone check failed", 
                "CHECK_FAILED"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Extract client IP address from request, handling proxies
     */
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
} 