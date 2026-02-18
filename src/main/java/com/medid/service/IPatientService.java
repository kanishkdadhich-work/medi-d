package com.medid.service;

import com.medid.dto.PatientRequestDTO;
import com.medid.dto.PatientResponseDTO;

/**
 * Service interface for patient-related operations.
 * Defines the contract for patient management functionality.
 */
public interface IPatientService {

    /**
     * Retrieve a patient by ID
     * @param id Patient ID
     * @return PatientResponseDTO containing patient information
     * @throws com.medid.exception.ResourceNotFoundException if patient not found
     * @throws com.medid.exception.InvalidRequestException if ID is invalid
     */
    PatientResponseDTO getPatient(Long id);

    /**
     * Create a new patient
     * @param patientRequestDTO Patient creation request with validation
     * @return PatientResponseDTO containing created patient information
     * @throws com.medid.exception.InvalidRequestException if request data is invalid
     */
    PatientResponseDTO createPatient(PatientRequestDTO patientRequestDTO);
}
