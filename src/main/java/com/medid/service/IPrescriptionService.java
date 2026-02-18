package com.medid.service;

import com.medid.dto.PrescriptionRequestDTO;
import com.medid.dto.PrescriptionResponseDTO;

import java.util.List;

/**
 * Service interface for prescription-related operations.
 * Manages prescription creation, workflow, and retrieval.
 */
public interface IPrescriptionService {

    /**
     * Retrieve a prescription by ID
     * @param id Prescription ID
     * @return PrescriptionResponseDTO containing prescription information
     * @throws com.medid.exception.ResourceNotFoundException if prescription not found
     */
    PrescriptionResponseDTO getPrescriptionById(Long id);

    /**
     * Get prescription by appointment ID (OneToOne relationship)
     * @param appointmentId Appointment ID
     * @return PrescriptionResponseDTO if prescription exists
     * @throws com.medid.exception.ResourceNotFoundException if prescription not found
     */
    PrescriptionResponseDTO getPrescriptionByAppointment(Long appointmentId);

    /**
     * Create a new prescription
     * @param prescriptionRequestDTO Prescription creation request
     * @return PrescriptionResponseDTO containing created prescription information
     * @throws com.medid.exception.InvalidRequestException if request data is invalid
     * @throws com.medid.exception.ResourceNotFoundException if appointment not found
     */
    PrescriptionResponseDTO createPrescription(PrescriptionRequestDTO prescriptionRequestDTO);

    /**
     * Update an existing prescription
     * @param id Prescription ID
     * @param prescriptionRequestDTO Updated prescription data
     * @return PrescriptionResponseDTO containing updated prescription information
     * @throws com.medid.exception.ResourceNotFoundException if prescription not found
     */
    PrescriptionResponseDTO updatePrescription(Long id, PrescriptionRequestDTO prescriptionRequestDTO);

    /**
     * Get all prescriptions with a specific status
     * @param status Prescription status (e.g., PENDING, DISPENSED, COMPLETED)
     * @return List of prescriptions with the specified status
     */
    List<PrescriptionResponseDTO> getPrescriptionsByStatus(String status);

    /**
     * Get all pending prescriptions
     * @return List of pending prescriptions (ordered by creation date ascending)
     */
    List<PrescriptionResponseDTO> getPendingPrescriptions();

    /**
     * Get all dispensed prescriptions
     * @return List of dispensed prescriptions (ordered by creation date descending)
     */
    List<PrescriptionResponseDTO> getDispensedPrescriptions();

    /**
     * Get all prescriptions for a specific patient
     * @param patientId Patient ID
     * @return List of patient's prescriptions (ordered by creation date descending)
     */
    List<PrescriptionResponseDTO> getPrescriptionsByPatient(Long patientId);

    /**
     * Count pending prescriptions for a patient
     * @param patientId Patient ID
     * @return Number of pending prescriptions
     */
    Long countPendingByPatient(Long patientId);

    /**
     * Delete a prescription by ID
     * @param id Prescription ID
     * @throws com.medid.exception.ResourceNotFoundException if prescription not found
     */
    void deletePrescription(Long id);
}
