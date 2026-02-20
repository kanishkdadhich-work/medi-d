package com.medid.controller;

import com.medid.dto.PrescriptionRequestDTO;
import com.medid.dto.PrescriptionResponseDTO;
import com.medid.service.IPrescriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/prescriptions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DOCTOR') or hasRole('RECEPTIONIST')")
public class PrescriptionController {

    private final IPrescriptionService prescriptionService;

    @GetMapping("/{id}")
    public ResponseEntity<PrescriptionResponseDTO> getPrescriptionById(@PathVariable Long id) {
        log.info("Fetching prescription with ID: {}", id);
        PrescriptionResponseDTO prescription = prescriptionService.getPrescriptionById(id);
        return ResponseEntity.ok(prescription);
    }

    @GetMapping("/appointment/{appointmentId}")
    public ResponseEntity<PrescriptionResponseDTO> getPrescriptionByAppointment(@PathVariable Long appointmentId) {
        log.info("Fetching prescription for appointment: {}", appointmentId);
        PrescriptionResponseDTO prescription = prescriptionService.getPrescriptionByAppointment(appointmentId);
        return ResponseEntity.ok(prescription);
    }

    @PostMapping
    public ResponseEntity<PrescriptionResponseDTO> createPrescription(@Valid @RequestBody PrescriptionRequestDTO prescriptionRequestDTO) {
        log.info("Creating new prescription for appointment: {}", prescriptionRequestDTO.getAppointmentId());
        PrescriptionResponseDTO createdPrescription = prescriptionService.createPrescription(prescriptionRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPrescription);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PrescriptionResponseDTO> updatePrescription(@PathVariable Long id, @Valid @RequestBody PrescriptionRequestDTO prescriptionRequestDTO) {
        log.info("Updating prescription with ID: {}", id);
        PrescriptionResponseDTO updatedPrescription = prescriptionService.updatePrescription(id, prescriptionRequestDTO);
        return ResponseEntity.ok(updatedPrescription);
    }

    @GetMapping("/by-status/{status}")
    public ResponseEntity<List<PrescriptionResponseDTO>> getPrescriptionsByStatus(@PathVariable String status) {
        log.info("Fetching prescriptions with status: {}", status);
        List<PrescriptionResponseDTO> prescriptions = prescriptionService.getPrescriptionsByStatus(status);
        return ResponseEntity.ok(prescriptions);
    }

    @GetMapping("/pending")
    public ResponseEntity<List<PrescriptionResponseDTO>> getPendingPrescriptions() {
        log.info("Fetching all pending prescriptions");
        List<PrescriptionResponseDTO> prescriptions = prescriptionService.getPendingPrescriptions();
        return ResponseEntity.ok(prescriptions);
    }

    @GetMapping("/dispensed")
    public ResponseEntity<List<PrescriptionResponseDTO>> getDispensedPrescriptions() {
        log.info("Fetching all dispensed prescriptions");
        List<PrescriptionResponseDTO> prescriptions = prescriptionService.getDispensedPrescriptions();
        return ResponseEntity.ok(prescriptions);
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<PrescriptionResponseDTO>> getPrescriptionsByPatient(@PathVariable Long patientId) {
        log.info("Fetching prescriptions for patient: {}", patientId);
        List<PrescriptionResponseDTO> prescriptions = prescriptionService.getPrescriptionsByPatient(patientId);
        return ResponseEntity.ok(prescriptions);
    }

    @GetMapping("/patient/{patientId}/pending-count")
    public ResponseEntity<Long> countPendingByPatient(@PathVariable Long patientId) {
        log.info("Counting pending prescriptions for patient: {}", patientId);
        Long count = prescriptionService.countPendingByPatient(patientId);
        return ResponseEntity.ok(count);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePrescription(@PathVariable Long id) {
        log.info("Deleting prescription with ID: {}", id);
        prescriptionService.deletePrescription(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/dispense")
    public ResponseEntity<PrescriptionResponseDTO> dispensePrescription(@PathVariable("id") Long id) {
        log.info("Attempting to dispense prescription with ID: {}", id);
        PrescriptionResponseDTO updated = prescriptionService.dispensePrescription(id);
        return ResponseEntity.ok(updated);
    }
}