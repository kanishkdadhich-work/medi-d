package com.medid.controller;

import com.medid.dto.PatientViewDTO;
import com.medid.dto.PagedResponse;
import com.medid.entity.Patient;
import com.medid.entity.User;
import com.medid.enums.Role;
import com.medid.service.IPatientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/patients")
@Slf4j
public class PatientController {

    @Autowired
    private IPatientService patientService;

    @PostMapping("/register")
    public ResponseEntity<Patient> register(@RequestBody Patient patient) {
        log.debug("Registering patient fullName={}, phone={}", patient.getFullName(), patient.getPhoneNumber());
        return ResponseEntity.ok(patientService.registerPatient(patient));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PatientViewDTO> getProfile(@PathVariable Long id, Authentication authentication) {
        log.debug("Fetching patient profile id={}", id);
        Patient patient = patientService.getPatientById(id);
        return ResponseEntity.ok(toView(patient, hasMedicalAccess(authentication)));
    }

    @GetMapping
    public ResponseEntity<List<PatientViewDTO>> getAll(Authentication authentication) {
        boolean includeMedicalBlob = hasMedicalAccess(authentication);
        log.debug("Fetching all patients includeMedicalBlob={}", includeMedicalBlob);
        List<PatientViewDTO> result = patientService.getAllPatients().stream()
                .map(p -> toView(p, includeMedicalBlob))
                .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/paged")
    public ResponseEntity<PagedResponse<PatientViewDTO>> getAllPaged(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        boolean includeMedicalBlob = hasMedicalAccess(authentication);
        Sort sort = "asc".equalsIgnoreCase(direction)
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<PatientViewDTO> mapped = patientService.getAllPatients(pageable).map(p -> toView(p, includeMedicalBlob));

        return ResponseEntity.ok(new PagedResponse<>(
                mapped.getContent(),
                mapped.getNumber(),
                mapped.getSize(),
                mapped.getTotalElements(),
                mapped.getTotalPages(),
                mapped.isFirst(),
                mapped.isLast()
        ));
    }

    @GetMapping("/by-phone")
    public ResponseEntity<?> getByPhone(@RequestParam String phoneNumber, Authentication authentication) {
        log.debug("Searching patient by phone={}", phoneNumber);
        Patient patient = patientService.getByPhoneNumber(phoneNumber);

        if (patient == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(toView(patient, hasMedicalAccess(authentication)));
    }

    @GetMapping("/search")
    public ResponseEntity<List<PatientViewDTO>> search(@RequestParam String q, Authentication authentication) {
        boolean includeMedicalBlob = hasMedicalAccess(authentication);
        log.debug("Searching patients with query={}", q);
        List<PatientViewDTO> result = patientService.searchPatients(q).stream()
                .map(p -> toView(p, includeMedicalBlob))
                .toList();
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{id}/medical-blob")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<PatientViewDTO> updateMedicalBlob(@PathVariable Long id,
                                                            @RequestBody Map<String, String> payload,
                                                            Authentication authentication) {
        String medicalHistoryBlob = payload.getOrDefault("medicalHistoryBlob", "");
        Patient updated = patientService.updateMedicalBlob(id, medicalHistoryBlob);
        return ResponseEntity.ok(toView(updated, hasMedicalAccess(authentication)));
    }

    private boolean hasMedicalAccess(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            return false;
        }
        return user.getRole() == Role.DOCTOR || user.getRole() == Role.ADMIN;
    }

    private PatientViewDTO toView(Patient patient, boolean includeMedicalBlob) {
        return new PatientViewDTO(
                patient.getPatientId(),
                patient.getFullName(),
                patient.getPhoneNumber(),
                patient.getGender(),
                includeMedicalBlob ? patient.getMedicalHistoryBlob() : null,
                patient.getCreatedAt()
        );
    }
}
