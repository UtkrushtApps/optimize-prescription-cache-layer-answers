package com.example.prescription.service;

import com.example.prescription.dto.PrescriptionDto;
import com.example.prescription.dto.UpdatePrescriptionRequest;

public interface PrescriptionService {

    /**
     * Read a prescription using a cache-first strategy with resilient fallbacks.
     */
    PrescriptionDto getPrescription(Long id);

    /**
     * Safely update a prescription using distributed locking and cache invalidation.
     */
    PrescriptionDto updatePrescription(Long id, UpdatePrescriptionRequest request);
}
