package com.example.prescription.exception;

public class PrescriptionLockedException extends RuntimeException {

    private final Long id;

    public PrescriptionLockedException(Long id) {
        super("Prescription is currently being updated: " + id);
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
