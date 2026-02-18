package com.medid.service;

import com.medid.dto.PatientRequestDTO;
import com.medid.dto.PatientResponseDTO;
import com.medid.entity.Patient;
import com.medid.exception.InvalidRequestException;
import com.medid.exception.ResourceNotFoundException;
import com.medid.repository.PatientRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PatientService {

    @Autowired
    private PatientRepository patientRepository;

    /**
     * Retrieve a patient by ID
     * @param id Patient ID
     * @return PatientResponseDTO
     * @throws ResourceNotFoundException if patient not found
     */
    public PatientResponseDTO getPatient(Long id) {
        if (id == null || id <= 0) {
            throw new InvalidRequestException("Patient ID must be a positive number");
        }

        return patientRepository.findById(id)
                .map(patient -> {
                    log.info("Patient found with ID: {}", id);
                    return new PatientResponseDTO(patient.getId(), patient.getName());
                })
                .orElseThrow(() -> {
                    log.warn("Patient not found with ID: {}", id);
                    return new ResourceNotFoundException("Patient not found with ID: " + id);
                });
    }

    /**
     * Create a new patient
     * @param patientRequestDTO Patient creation request
     * @return PatientResponseDTO
     * @throws InvalidRequestException if request data is invalid
     */
    public PatientResponseDTO createPatient(PatientRequestDTO patientRequestDTO) {
        try {
            // Validate request data
            if (patientRequestDTO == null) {
                throw new InvalidRequestException("Patient request cannot be null");
            }

            if (patientRequestDTO.getName() == null || patientRequestDTO.getName().trim().isEmpty()) {
                throw new InvalidRequestException("Patient name cannot be empty");
            }

            if (patientRequestDTO.getContact() == null || patientRequestDTO.getContact().trim().isEmpty()) {
                throw new InvalidRequestException("Patient contact cannot be empty");
            }

            // Create and save patient entity
            Patient patient = new Patient();
            patient.setName(patientRequestDTO.getName().trim());
            patient.setContact(patientRequestDTO.getContact().trim());

            Patient savedPatient = patientRepository.save(patient);
            
            log.info("Patient created successfully with ID: {}", savedPatient.getId());
            
            return new PatientResponseDTO(savedPatient.getId(), savedPatient.getName());
        } catch (InvalidRequestException ex) {
            log.warn("Invalid request for patient creation: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Error creating patient: {}", ex.getMessage(), ex);
            throw new InvalidRequestException("Failed to create patient: " + ex.getMessage(), ex);
        }
    }
}

