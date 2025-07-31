package com.example.session_demo.controller;

import com.example.session_demo.dto.ApiResponse;
import com.example.session_demo.dto.LoginRequestDTO;
import com.example.session_demo.dto.LoginResponseDTO;
import com.example.session_demo.service.AuthService;
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
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Authentication endpoints for healthcare providers")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(
        summary = "Login provider",
        description = "Authenticate a healthcare provider and return JWT token"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication failed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Validation failed")
    })
    public ResponseEntity<ApiResponse<LoginResponseDTO>> login(
            @Valid @RequestBody LoginRequestDTO loginRequest,
            BindingResult bindingResult,
            HttpServletRequest request) {

        log.info("Login request received from IP: {}", getClientIpAddress(request));

        try {
            // Check for validation errors
            if (bindingResult.hasErrors()) {
                Map<String, List<String>> errors = new HashMap<>();
                for (FieldError error : bindingResult.getFieldErrors()) {
                    errors.computeIfAbsent(error.getField(), k -> new java.util.ArrayList<>())
                           .add(error.getDefaultMessage());
                }

                ApiResponse<LoginResponseDTO> response = ApiResponse.<LoginResponseDTO>builder()
                    .success(false)
                    .message("Validation failed")
                    .errorCode("VALIDATION_ERROR")
                    .errors(errors)
                    .build();

                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
            }

            LoginResponseDTO loginResponse = authService.login(loginRequest);

            ApiResponse<LoginResponseDTO> response = ApiResponse.<LoginResponseDTO>builder()
                .success(true)
                .message("Login successful")
                .data(loginResponse)
                .build();

            return ResponseEntity.ok(response);

        } catch (AuthService.AuthenticationException e) {
            log.warn("Authentication failed: {}", e.getMessage());
            
            ApiResponse<LoginResponseDTO> response = ApiResponse.<LoginResponseDTO>builder()
                .success(false)
                .message(e.getMessage())
                .errorCode("AUTHENTICATION_FAILED")
                .build();

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);

        } catch (Exception e) {
            log.error("Unexpected error during login: {}", e.getMessage(), e);
            
            ApiResponse<LoginResponseDTO> response = ApiResponse.<LoginResponseDTO>builder()
                .success(false)
                .message("Internal server error occurred")
                .errorCode("INTERNAL_ERROR")
                .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

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