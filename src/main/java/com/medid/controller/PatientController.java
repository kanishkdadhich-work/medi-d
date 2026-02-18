package com.medid.controller;

import com.medid.dto.PatientResponseDTO;
import com.medid.service.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class PatientController {

    @Autowired
    private PatientService patientService;

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Medi-D System is up and running!");
    }

    @GetMapping("/patients/{id}")
    public ResponseEntity<PatientResponseDTO> getPatient(@PathVariable Long id) {
        PatientResponseDTO patient = patientService.getPatient(id);
        return ResponseEntity.ok(patient);
    }
}
