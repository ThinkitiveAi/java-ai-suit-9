package com.example.session_demo.enums;

public enum ProviderSpecialization {
    ALLERGY_IMMUNOLOGY("Allergy and Immunology"),
    ANESTHESIOLOGY("Anesthesiology"),
    CARDIOLOGY("Cardiology"),
    DERMATOLOGY("Dermatology"),
    EMERGENCY_MEDICINE("Emergency Medicine"),
    FAMILY_MEDICINE("Family Medicine"),
    GASTROENTEROLOGY("Gastroenterology"),
    GENERAL_SURGERY("General Surgery"),
    HEMATOLOGY("Hematology"),
    INFECTIOUS_DISEASE("Infectious Disease"),
    INTERNAL_MEDICINE("Internal Medicine"),
    NEPHROLOGY("Nephrology"),
    NEUROLOGY("Neurology"),
    NEUROSURGERY("Neurosurgery"),
    OBSTETRICS_GYNECOLOGY("Obstetrics and Gynecology"),
    ONCOLOGY("Oncology"),
    OPHTHALMOLOGY("Ophthalmology"),
    ORTHOPEDIC_SURGERY("Orthopedic Surgery"),
    OTOLARYNGOLOGY("Otolaryngology"),
    PATHOLOGY("Pathology"),
    PEDIATRICS("Pediatrics"),
    PHYSICAL_MEDICINE("Physical Medicine and Rehabilitation"),
    PLASTIC_SURGERY("Plastic Surgery"),
    PSYCHIATRY("Psychiatry"),
    PULMONOLOGY("Pulmonology"),
    RADIOLOGY("Radiology"),
    RHEUMATOLOGY("Rheumatology"),
    UROLOGY("Urology"),
    NURSE_PRACTITIONER("Nurse Practitioner"),
    PHYSICIAN_ASSISTANT("Physician Assistant"),
    OTHER("Other");

    private final String displayName;

    ProviderSpecialization(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
} 