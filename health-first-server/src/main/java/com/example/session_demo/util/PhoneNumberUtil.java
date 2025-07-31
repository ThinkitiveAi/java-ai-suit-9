package com.example.session_demo.util;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import org.springframework.stereotype.Component;

@Component
public class PhoneNumberUtil {

    private final com.google.i18n.phonenumbers.PhoneNumberUtil phoneUtil = 
        com.google.i18n.phonenumbers.PhoneNumberUtil.getInstance();

    /**
     * Validate and normalize phone number to international format
     */
    public String normalizePhoneNumber(String phoneNumber) throws NumberParseException {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be null or empty");
        }

        // Default to US region if no country code is provided
        String region = phoneNumber.startsWith("+") ? null : "US";
        
        PhoneNumber parsedNumber = phoneUtil.parse(phoneNumber, region);
        
        if (!phoneUtil.isValidNumber(parsedNumber)) {
            throw new NumberParseException(
                NumberParseException.ErrorType.INVALID_COUNTRY_CODE,
                "Invalid phone number format"
            );
        }

        return phoneUtil.format(parsedNumber, PhoneNumberFormat.E164);
    }

    /**
     * Validate phone number format
     */
    public boolean isValidPhoneNumber(String phoneNumber) {
        try {
            return normalizePhoneNumber(phoneNumber) != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Format phone number for display
     */
    public String formatForDisplay(String phoneNumber) {
        try {
            String region = phoneNumber.startsWith("+") ? null : "US";
            PhoneNumber parsedNumber = phoneUtil.parse(phoneNumber, region);
            return phoneUtil.format(parsedNumber, PhoneNumberFormat.NATIONAL);
        } catch (NumberParseException e) {
            return phoneNumber; // Return original if parsing fails
        }
    }
} 