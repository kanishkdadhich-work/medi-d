package com.medid.controller;

import com.medid.dto.PrescriptionItemRequestDTO;
import com.medid.dto.PrescriptionItemResponseDTO;
import com.medid.service.IPrescriptionItemService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for prescription item management endpoints.
 * Provides API endpoints for prescription item operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/prescription-items")
@PreAuthorize("hasRole('DOCTOR') or hasRole('PHARMACIST')")
public class PrescriptionItemController {

    @Autowired
    private IPrescriptionItemService prescriptionItemService;

    @GetMapping("/{id}")
    public ResponseEntity<PrescriptionItemResponseDTO> getPrescriptionItemById(@PathVariable Long id) {
        log.info("Fetching prescription item with ID: {}", id);
        PrescriptionItemResponseDTO item = prescriptionItemService.getPrescriptionItemById(id);
        return ResponseEntity.ok(item);
    }

    @PostMapping
    public ResponseEntity<PrescriptionItemResponseDTO> createPrescriptionItem(@Valid @RequestBody PrescriptionItemRequestDTO prescriptionItemRequestDTO) {
        log.info("Creating new prescription item for prescription: {}", prescriptionItemRequestDTO.getPrescriptionId());
        PrescriptionItemResponseDTO createdItem = prescriptionItemService.createPrescriptionItem(prescriptionItemRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdItem);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PrescriptionItemResponseDTO> updatePrescriptionItem(@PathVariable Long id, @Valid @RequestBody PrescriptionItemRequestDTO prescriptionItemRequestDTO) {
        log.info("Updating prescription item with ID: {}", id);
        PrescriptionItemResponseDTO updatedItem = prescriptionItemService.updatePrescriptionItem(id, prescriptionItemRequestDTO);
        return ResponseEntity.ok(updatedItem);
    }

    @GetMapping("/prescription/{prescriptionId}")
    public ResponseEntity<List<PrescriptionItemResponseDTO>> getItemsByPrescription(@PathVariable Long prescriptionId) {
        log.info("Fetching items for prescription: {}", prescriptionId);
        List<PrescriptionItemResponseDTO> items = prescriptionItemService.getItemsByPrescription(prescriptionId);
        return ResponseEntity.ok(items);
    }

    @GetMapping("/medicine/{medicineId}")
    public ResponseEntity<List<PrescriptionItemResponseDTO>> getItemsByMedicine(@PathVariable Long medicineId) {
        log.info("Fetching items for medicine: {}", medicineId);
        List<PrescriptionItemResponseDTO> items = prescriptionItemService.getItemsByMedicine(medicineId);
        return ResponseEntity.ok(items);
    }

    @GetMapping("/medicine/{medicineId}/pending")
    public ResponseEntity<List<PrescriptionItemResponseDTO>> getPendingItemsByMedicine(@PathVariable Long medicineId) {
        log.info("Fetching pending items for medicine: {}", medicineId);
        List<PrescriptionItemResponseDTO> items = prescriptionItemService.getPendingItemsByMedicine(medicineId);
        return ResponseEntity.ok(items);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePrescriptionItem(@PathVariable Long id) {
        log.info("Deleting prescription item with ID: {}", id);
        prescriptionItemService.deletePrescriptionItem(id);
        return ResponseEntity.noContent().build();
    }
}
