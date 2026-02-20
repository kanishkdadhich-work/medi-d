package com.medid.controller;

import com.medid.dto.PharmacistPrescriptionDTO;
import com.medid.service.IPharmacyService;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/pharmacy")
public class PharmacyController {

    @Autowired
    private IPharmacyService pharmacyService;

    @GetMapping("/prescriptions/pending")
    @PreAuthorize("hasRole('PHARMACIST')")
    public ResponseEntity<List<PharmacistPrescriptionDTO>> getPending() {
        log.info("Pharmacist fetching pending prescriptions");
        return ResponseEntity.ok(pharmacyService.getPendingPrescriptionsForPharmacist());
    }

    @PostMapping("/prescriptions/{id}/dispense")
    @PreAuthorize("hasRole('PHARMACIST')")
    public ResponseEntity<Void> dispense(@PathVariable("id") @NotNull Long id) {
        log.info("Pharmacist dispensing prescription: {}", id);
        pharmacyService.dispensePrescription(id);
        return ResponseEntity.ok().build();
    }
}
