package com.example.session_demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatientAddress {

    @NotBlank(message = "Street address is required")
    @Size(max = 200, message = "Street address must not exceed 200 characters")
    @Column(name = "address_street")
    private String street;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City must not exceed 100 characters")
    @Column(name = "address_city")
    private String city;

    @NotBlank(message = "State is required")
    @Size(max = 50, message = "State must not exceed 50 characters")
    @Column(name = "address_state")
    private String state;

    @NotBlank(message = "ZIP code is required")
    @Pattern(regexp = "^\\d{5}(-\\d{4})?$", message = "ZIP code must be in format 12345 or 12345-6789")
    @Column(name = "address_zip")
    private String zip;

    @Size(max = 50, message = "Country must not exceed 50 characters")
    @Column(name = "address_country")
    private String country = "US";

    public String getFullAddress() {
        return String.format("%s, %s, %s %s, %s", street, city, state, zip, country);
    }

    // Helper methods for address normalization
    public void setStreet(String street) {
        this.street = street != null ? street.trim() : null;
    }

    public void setCity(String city) {
        this.city = city != null ? city.trim() : null;
    }

    public void setState(String state) {
        this.state = state != null ? state.trim().toUpperCase() : null;
    }

    public void setZip(String zip) {
        this.zip = zip != null ? zip.trim() : null;
    }

    public void setCountry(String country) {
        this.country = country != null ? country.trim().toUpperCase() : "US";
    }
} 