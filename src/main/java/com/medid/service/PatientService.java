package com.medid.service;

import com.medid.dto.PatientResponseDTO;
import com.medid.entity.Patient;
import com.medid.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PatientService {

    @Autowired
    private PatientRepository patientRepository;

    public PatientResponseDTO getPatient(Long id) {
        return patientRepository.findById(id)
                .map(patient -> new PatientResponseDTO(patient.getId(), patient.getName()))
                .orElse(new PatientResponseDTO(id, "Patient not found"));
    }
}
