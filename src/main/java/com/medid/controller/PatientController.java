package com.medid.controller;

import com.medid.dto.PatientRequestDTO;
import com.medid.dto.PatientResponseDTO;
import com.medid.service.PatientService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api")
public class PatientController {

    @Autowired
    private PatientService patientService;

    /**
     * Health check endpoint
     * @return Health status message
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        log.info("Health check endpoint called");
        return ResponseEntity.ok("Medi-D System is up and running!");
    }

    /**
     * Get a patient by ID
     * @param id Patient ID
     * @return PatientResponseDTO with patient details
     */
    @GetMapping("/patients/{id}")
    public ResponseEntity<PatientResponseDTO> getPatient(@PathVariable Long id) {
        log.info("Fetching patient with ID: {}", id);
        PatientResponseDTO patient = patientService.getPatient(id);
        return ResponseEntity.ok(patient);
    }

    /**
     * Create a new patient
     * @param patientRequestDTO Patient creation request with name and contact
     * @return PatientResponseDTO with created patient details and 201 Created status
     */
    @PostMapping("/patients")
    public ResponseEntity<PatientResponseDTO> createPatient(@Valid @RequestBody PatientRequestDTO patientRequestDTO) {
        log.info("Creating new patient with name: {}", patientRequestDTO.getName());
        PatientResponseDTO createdPatient = patientService.createPatient(patientRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPatient);
    }
}

