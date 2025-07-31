package com.example.session_demo.enums;

import lombok.Getter;

@Getter
public enum Gender {
    MALE("male", "Male"),
    FEMALE("female", "Female"),
    OTHER("other", "Other"),
    PREFER_NOT_TO_SAY("prefer_not_to_say", "Prefer not to say");

    private final String code;
    private final String displayName;

    Gender(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public static Gender fromCode(String code) {
        if (code == null) {
            return null;
        }
        
        for (Gender gender : Gender.values()) {
            if (gender.code.equalsIgnoreCase(code)) {
                return gender;
            }
        }
        
        throw new IllegalArgumentException("Invalid gender code: " + code);
    }

    @Override
    public String toString() {
        return displayName;
    }
} 