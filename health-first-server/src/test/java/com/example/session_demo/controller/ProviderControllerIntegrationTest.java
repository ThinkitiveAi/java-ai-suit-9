package com.example.session_demo.controller;

import com.example.session_demo.dto.ProviderRegisterDTO;
import com.example.session_demo.enums.ProviderSpecialization;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class ProviderControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @Test
    void registerProvider_ValidData_ReturnsCreated() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // Prepare valid registration DTO
        ProviderRegisterDTO registerDTO = new ProviderRegisterDTO();
        registerDTO.setFirstName("Jane");
        registerDTO.setLastName("Smith");
        registerDTO.setEmail("jane.smith@clinic.com");
        registerDTO.setPhoneNumber("+19876543210");
        registerDTO.setPassword("SecurePassword123!");
        registerDTO.setConfirmPassword("SecurePassword123!");
        registerDTO.setSpecialization(ProviderSpecialization.FAMILY_MEDICINE);
        registerDTO.setLicenseNumber("MD987654321");
        registerDTO.setYearsOfExperience(5);

        ProviderRegisterDTO.ClinicAddressDTO addressDTO = new ProviderRegisterDTO.ClinicAddressDTO();
        addressDTO.setStreet("456 Healthcare Blvd");
        addressDTO.setCity("Boston");
        addressDTO.setState("MA");
        addressDTO.setZip("02101");
        registerDTO.setClinicAddress(addressDTO);

        // Perform request and verify response
        mockMvc.perform(post("/api/v1/provider/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Provider registered successfully. Verification email sent."))
                .andExpect(jsonPath("$.data.email").value("jane.smith@clinic.com"));
    }

    @Test
    void registerProvider_InvalidEmail_ReturnsUnprocessableEntity() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // Prepare DTO with invalid email
        ProviderRegisterDTO registerDTO = new ProviderRegisterDTO();
        registerDTO.setFirstName("Jane");
        registerDTO.setLastName("Smith");
        registerDTO.setEmail("invalid-email"); // Invalid email format
        registerDTO.setPhoneNumber("+19876543210");
        registerDTO.setPassword("SecurePassword123!");
        registerDTO.setConfirmPassword("SecurePassword123!");
        registerDTO.setSpecialization(ProviderSpecialization.FAMILY_MEDICINE);
        registerDTO.setLicenseNumber("MD987654321");
        registerDTO.setYearsOfExperience(5);

        ProviderRegisterDTO.ClinicAddressDTO addressDTO = new ProviderRegisterDTO.ClinicAddressDTO();
        addressDTO.setStreet("456 Healthcare Blvd");
        addressDTO.setCity("Boston");
        addressDTO.setState("MA");
        addressDTO.setZip("02101");
        registerDTO.setClinicAddress(addressDTO);

        // Perform request and verify validation error
        mockMvc.perform(post("/api/v1/provider/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerDTO)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void registerProvider_MissingRequiredField_ReturnsUnprocessableEntity() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // Prepare DTO with missing first name
        ProviderRegisterDTO registerDTO = new ProviderRegisterDTO();
        // Missing firstName
        registerDTO.setLastName("Smith");
        registerDTO.setEmail("jane.smith@clinic.com");
        registerDTO.setPhoneNumber("+19876543210");
        registerDTO.setPassword("SecurePassword123!");
        registerDTO.setConfirmPassword("SecurePassword123!");
        registerDTO.setSpecialization(ProviderSpecialization.FAMILY_MEDICINE);
        registerDTO.setLicenseNumber("MD987654321");
        registerDTO.setYearsOfExperience(5);

        // Perform request and verify validation error
        mockMvc.perform(post("/api/v1/provider/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerDTO)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }
} 