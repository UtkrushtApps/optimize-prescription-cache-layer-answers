package com.example.prescription.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdatePrescriptionRequest {

    @NotBlank
    private String medication;

    @NotBlank
    private String dosage;

    private String instructions;

    public String getMedication() {
        return medication;
    }

    public void setMedication(String medication) {
        this.medication = medication;
    }

    public String getDosage() {
        return dosage;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }
}
