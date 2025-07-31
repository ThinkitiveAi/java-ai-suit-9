package com.example.session_demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponseDTO {
    
    private String token;
    private String tokenType;
    private Long expiresIn;
    private Long id;
    private UUID uuid;
    private String email;
    private String firstName;
    private String lastName;
    private LocalDateTime loginTime;
} 