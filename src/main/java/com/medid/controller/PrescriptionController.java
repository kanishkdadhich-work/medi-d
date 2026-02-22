package com.medid.controller;

import com.medid.dto.LatestConsultationDTO;
import com.medid.dto.PrescriptionRequest;
import com.medid.entity.Prescription;
import com.medid.service.IPrescriptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prescriptions")
@Slf4j
public class PrescriptionController {

    @Autowired
    private IPrescriptionService prescriptionService;

    // Requirement 2.1: Doctor saves the prescription after consultation
    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<?> createPrescription(@RequestBody PrescriptionRequest request) {
        log.debug("Controller request create prescription appointmentId={}", request.getAppointmentId());
        return ResponseEntity.ok(prescriptionService.createPrescription(request));
    }

    // Requirement 2.6: List all prescriptions waiting for medicine
    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public ResponseEntity<List<Prescription>> getPendingPrescriptions() {
        log.debug("Controller request get pending prescriptions");
        return ResponseEntity.ok(prescriptionService.getPendingPrescriptions());
    }

    @GetMapping("/latest")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<LatestConsultationDTO> getLatestByPatient(@RequestParam Long patientId) {
        Prescription prescription = prescriptionService.getLatestByPatientId(patientId);
        LatestConsultationDTO dto = new LatestConsultationDTO(
                prescription.getPrescriptionId(),
                prescription.getAppointment() == null ? null : prescription.getAppointment().getAppointmentId(),
                prescription.getAppointment() == null || prescription.getAppointment().getPatient() == null ? null : prescription.getAppointment().getPatient().getPatientId(),
                prescription.getAppointment() == null || prescription.getAppointment().getPatient() == null ? "-" : prescription.getAppointment().getPatient().getFullName(),
                prescription.getDiagnosisNotes(),
                prescription.getCreatedAt(),
                prescription.getItems() == null ? List.of() : prescription.getItems().stream()
                        .map(item -> item.getMedicine().getName() + " x " + item.getQuantity())
                        .toList()
        );
        return ResponseEntity.ok(dto);
    }


}
