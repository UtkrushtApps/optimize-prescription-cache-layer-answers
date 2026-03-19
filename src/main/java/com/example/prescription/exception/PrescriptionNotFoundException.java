package com.example.prescription.exception;

public class PrescriptionNotFoundException extends RuntimeException {

    private final Long id;

    public PrescriptionNotFoundException(Long id) {
        super("Prescription not found: " + id);
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
