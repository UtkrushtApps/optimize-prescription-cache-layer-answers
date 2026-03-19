package com.example.prescription.service;

import com.example.prescription.cache.PrescriptionCacheService;
import com.example.prescription.dto.PrescriptionDto;
import com.example.prescription.dto.UpdatePrescriptionRequest;
import com.example.prescription.exception.PrescriptionLockedException;
import com.example.prescription.exception.PrescriptionNotFoundException;
import com.example.prescription.exception.ServiceUnavailableException;
import com.example.prescription.lock.DistributedLockService;
import com.example.prescription.messaging.PrescriptionEventPublisher;
import com.example.prescription.model.Prescription;
import com.example.prescription.repository.PrescriptionRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class PrescriptionServiceImpl implements PrescriptionService {

    private static final Logger log = LoggerFactory.getLogger(PrescriptionServiceImpl.class);

    private static final String READ_CB = "prescriptionRead";
    private static final String WRITE_CB = "prescriptionWrite";

    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionCacheService cacheService;
    private final DistributedLockService lockService;
    private final PrescriptionEventPublisher eventPublisher;

    public PrescriptionServiceImpl(PrescriptionRepository prescriptionRepository,
                                   PrescriptionCacheService cacheService,
                                   DistributedLockService lockService,
                                   PrescriptionEventPublisher eventPublisher) {
        this.prescriptionRepository = prescriptionRepository;
        this.cacheService = cacheService;
        this.lockService = lockService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Timed(value = "prescription.read", description = "Time taken to read a prescription")
    @Retry(name = READ_CB)
    @CircuitBreaker(name = READ_CB, fallbackMethod = "getPrescriptionFromCacheOnly")
    @Transactional(readOnly = true)
    public PrescriptionDto getPrescription(Long id) {
        // Cache-aside strategy: try Redis cache first, then DB, then populate cache.
        return cacheService.get(id)
                .orElseGet(() -> {
                    log.debug("Cache miss for prescription {}. Loading from DB.", id);
                    Prescription prescription = prescriptionRepository.findById(id)
                            .orElseThrow(() -> new PrescriptionNotFoundException(id));
                    PrescriptionDto dto = PrescriptionDto.fromEntity(prescription);
                    cacheService.put(dto);
                    return dto;
                });
    }

    /**
     * Fallback used by the circuit breaker for read operations.
     * Attempts to serve the request from cache only when the DB is unhealthy.
     */
    @SuppressWarnings("unused")
    public PrescriptionDto getPrescriptionFromCacheOnly(Long id, Throwable cause) {
        log.warn("Falling back to cache-only read for prescription {} due to: {}", id, cause.toString());
        return cacheService.get(id)
                .orElseThrow(() -> new ServiceUnavailableException(
                        "Prescription service temporarily unavailable for id=" + id));
    }

    @Override
    @Timed(value = "prescription.write", description = "Time taken to update a prescription")
    @Retry(name = WRITE_CB)
    @CircuitBreaker(name = WRITE_CB, fallbackMethod = "updatePrescriptionFallback")
    @Transactional
    public PrescriptionDto updatePrescription(Long id, UpdatePrescriptionRequest request) {
        String lockKey = lockService.buildLockKeyForPrescription(id);
        String lockValue = UUID.randomUUID().toString();
        Duration lockTtl = Duration.ofSeconds(10);

        // Ensure only one writer at a time across all instances.
        boolean locked = lockService.tryLock(lockKey, lockValue, lockTtl);
        if (!locked) {
            log.info("Failed to acquire lock for prescription {}. Another writer is active.", id);
            throw new PrescriptionLockedException(id);
        }

        try {
            Prescription prescription = prescriptionRepository.findById(id)
                    .orElseThrow(() -> new PrescriptionNotFoundException(id));

            applyUpdate(prescription, request);

            try {
                Prescription saved = prescriptionRepository.saveAndFlush(prescription);
                PrescriptionDto dto = PrescriptionDto.fromEntity(saved);

                // Keep cache fresh and consistent with DB.
                cacheService.put(dto);

                // Publish change event (best-effort; failures are logged but do not fail the write).
                eventPublisher.publishPrescriptionUpdated(dto);

                return dto;
            } catch (OptimisticLockingFailureException e) {
                // Indicates concurrent modification despite locking (e.g. lock TTL edge cases).
                log.warn("Optimistic locking failure while updating prescription {}: {}", id, e.getMessage());
                throw e;
            }
        } finally {
            // Always attempt to release the distributed lock.
            lockService.unlock(lockKey, lockValue);
        }
    }

    /**
     * Fallback used by the circuit breaker for write operations.
     */
    @SuppressWarnings("unused")
    public PrescriptionDto updatePrescriptionFallback(Long id, UpdatePrescriptionRequest request, Throwable cause) {
        log.error("Update for prescription {} failed. Entering fallback. Cause: {}", id, cause.toString());
        throw new ServiceUnavailableException(
                "Unable to update prescription " + id + " due to downstream failure");
    }

    private void applyUpdate(Prescription prescription, UpdatePrescriptionRequest request) {
        // For simplicity, assume all fields are fully replaced. Real-world logic may be more granular.
        prescription.setMedication(request.getMedication());
        prescription.setDosage(request.getDosage());
        prescription.setInstructions(request.getInstructions());
        prescription.setUpdatedAt(Instant.now());
    }
}
