package com.example.session_demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenRefreshResponseDTO {
    
    private String accessToken;
    private String refreshToken; // New refresh token (rotation)
    private Long expiresIn;
    private Long refreshExpiresIn;
    private String tokenType;
} 