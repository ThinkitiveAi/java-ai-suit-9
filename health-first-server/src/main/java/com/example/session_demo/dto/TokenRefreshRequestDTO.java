package com.example.session_demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenRefreshRequestDTO {
    
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
} 