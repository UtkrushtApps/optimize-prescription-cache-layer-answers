package com.example.prescription.api;

import com.example.prescription.dto.PrescriptionDto;
import com.example.prescription.dto.UpdatePrescriptionRequest;
import com.example.prescription.service.PrescriptionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/prescriptions")
public class PrescriptionController {

    private static final Logger log = LoggerFactory.getLogger(PrescriptionController.class);

    private final PrescriptionService prescriptionService;

    public PrescriptionController(PrescriptionService prescriptionService) {
        this.prescriptionService = prescriptionService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<PrescriptionDto> getPrescription(@PathVariable("id") Long id) {
        log.debug("HTTP GET /api/prescriptions/{}", id);
        PrescriptionDto dto = prescriptionService.getPrescription(id);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PrescriptionDto> updatePrescription(@PathVariable("id") Long id,
                                                              @Valid @RequestBody UpdatePrescriptionRequest request) {
        log.debug("HTTP PUT /api/prescriptions/{}", id);
        PrescriptionDto dto = prescriptionService.updatePrescription(id, request);
        return ResponseEntity.ok(dto);
    }
}
