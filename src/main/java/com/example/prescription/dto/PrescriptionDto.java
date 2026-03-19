package com.example.prescription.dto;

import com.example.prescription.model.Prescription;

import java.time.Instant;

public class PrescriptionDto {

    private Long id;
    private Long patientId;
    private Long doctorId;
    private String medication;
    private String dosage;
    private String instructions;
    private Long version;
    private Instant createdAt;
    private Instant updatedAt;

    public static PrescriptionDto fromEntity(Prescription entity) {
        PrescriptionDto dto = new PrescriptionDto();
        dto.setId(entity.getId());
        dto.setPatientId(entity.getPatientId());
        dto.setDoctorId(entity.getDoctorId());
        dto.setMedication(entity.getMedication());
        dto.setDosage(entity.getDosage());
        dto.setInstructions(entity.getInstructions());
        dto.setVersion(entity.getVersion());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }

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

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
