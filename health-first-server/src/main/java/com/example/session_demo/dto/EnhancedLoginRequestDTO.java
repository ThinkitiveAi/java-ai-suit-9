package com.example.session_demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnhancedLoginRequestDTO {
    
    @NotBlank(message = "Identifier (email or phone) is required")
    private String identifier; // Can be email or phone number
    
    @NotBlank(message = "Password is required")
    private String password;
    
    private Boolean rememberMe = false;
    
    private String deviceInfo; // Optional device information
    
    // Validation method to check if identifier is email or phone
    public boolean isEmail() {
        return identifier != null && identifier.contains("@");
    }
    
    public boolean isPhoneNumber() {
        return identifier != null && !identifier.contains("@");
    }
} 