package com.medid.service;

import com.medid.entity.Patient;
import com.medid.repository.PatientRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Slf4j
public class PatientServiceImpl implements IPatientService {

    @Autowired
    private PatientRepository patientRepo;

    @Override
    public Patient registerPatient(Patient patient) {
        log.debug("Register patient request fullName={}, phone={}", patient.getFullName(), patient.getPhoneNumber());
        // Basic Validation (Requirement 1)
        if (patient.getPhoneNumber() == null || !patient.getPhoneNumber().matches("\\d{10}")) {
            throw new RuntimeException("Invalid Phone Number. Must be 10 digits.");
        }
        Patient saved = patientRepo.save(patient);
        log.debug("Patient registered patientId={}", saved.getPatientId());
        return saved;
    }

    @Override
    public Patient getPatientById(Long id) {
        log.debug("Get patient by id={}", id);
        return patientRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Patient not found"));
    }

    @Override
    public List<Patient> getAllPatients() {
        List<Patient> all = patientRepo.findAll();
        log.debug("Fetched all patients count={}", all.size());
        return all;
    }

    @Override
    public Patient getByPhoneNumber(String phoneNumber) {
        Patient patient = patientRepo.findByPhoneNumber(phoneNumber).orElse(null);
        log.debug("Get patient by phone={} found={}", phoneNumber, patient != null);
        return patient;
    }

    @Override
    public List<Patient> searchPatients(String query) {
        log.debug("Search patients query={}", query);
        if (query == null || query.isBlank()) {
            return List.of();
        }

        String trimmed = query.trim();
        Map<Long, Patient> unique = new LinkedHashMap<>();

        try {
            Long id = Long.parseLong(trimmed);
            patientRepo.findById(id).ifPresent(p -> unique.put(p.getPatientId(), p));
        } catch (NumberFormatException ignored) {
            // Non-numeric query: ignore ID search
        }

        List<Patient> byName = patientRepo.findTop10ByFullNameContainingIgnoreCaseOrderByFullNameAsc(trimmed);
        byName.forEach(p -> unique.putIfAbsent(p.getPatientId(), p));

        List<Patient> result = new ArrayList<>(unique.values());
        log.debug("Search patients query={} returned={} records", query, result.size());
        return result;
    }

    @Override
    public Patient updateMedicalBlob(Long patientId, String medicalHistoryBlob) {
        Patient patient = patientRepo.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found"));
        patient.setMedicalHistoryBlob(medicalHistoryBlob);
        Patient saved = patientRepo.save(patient);
        log.debug("Updated medical blob for patientId={}", patientId);
        return saved;
    }
}
