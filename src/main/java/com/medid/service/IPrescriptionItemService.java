package com.medid.service;

import com.medid.dto.PrescriptionItemRequestDTO;
import com.medid.dto.PrescriptionItemResponseDTO;

import java.util.List;

/**
 * Service interface for prescription item-related operations.
 * Manages individual medicine items within prescriptions.
 */
public interface IPrescriptionItemService {

    /**
     * Retrieve a prescription item by ID
     * @param id Prescription item ID
     * @return PrescriptionItemResponseDTO containing item information
     * @throws com.medid.exception.ResourceNotFoundException if item not found
     */
    PrescriptionItemResponseDTO getPrescriptionItemById(Long id);

    /**
     * Create a new prescription item
     * @param prescriptionItemRequestDTO Prescription item creation request
     * @return PrescriptionItemResponseDTO containing created item information
     * @throws com.medid.exception.InvalidRequestException if request data is invalid
     * @throws com.medid.exception.ResourceNotFoundException if prescription or medicine not found
     */
    PrescriptionItemResponseDTO createPrescriptionItem(PrescriptionItemRequestDTO prescriptionItemRequestDTO);

    /**
     * Update an existing prescription item
     * @param id Prescription item ID
     * @param prescriptionItemRequestDTO Updated item data
     * @return PrescriptionItemResponseDTO containing updated item information
     * @throws com.medid.exception.ResourceNotFoundException if item not found
     */
    PrescriptionItemResponseDTO updatePrescriptionItem(Long id, PrescriptionItemRequestDTO prescriptionItemRequestDTO);

    /**
     * Get all items in a specific prescription
     * @param prescriptionId Prescription ID
     * @return List of prescription items (ordered by ID ascending)
     */
    List<PrescriptionItemResponseDTO> getItemsByPrescription(Long prescriptionId);

    /**
     * Get all prescription items that reference a specific medicine
     * @param medicineId Medicine ID
     * @return List of prescription items (ordered by creation date descending)
     */
    List<PrescriptionItemResponseDTO> getItemsByMedicine(Long medicineId);

    /**
     * Get pending prescription items for a specific medicine
     * @param medicineId Medicine ID
     * @return List of items with pending prescriptions
     */
    List<PrescriptionItemResponseDTO> getPendingItemsByMedicine(Long medicineId);

    /**
     * Delete a prescription item by ID
     * @param id Prescription item ID
     * @throws com.medid.exception.ResourceNotFoundException if item not found
     */
    void deletePrescriptionItem(Long id);
}
