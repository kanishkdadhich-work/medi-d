package com.medid.service;

import com.medid.dto.MedicineRequestDTO;
import com.medid.dto.MedicineResponseDTO;

import java.time.LocalDate;
import java.util.List;

/**
 * Service interface for medicine-related operations.
 * Handles inventory management, stock tracking, and expiry monitoring.
 */
public interface IMedicineService {

    /**
     * Retrieve a medicine by ID
     * @param id Medicine ID
     * @return MedicineResponseDTO containing medicine information
     * @throws com.medid.exception.ResourceNotFoundException if medicine not found
     */
    MedicineResponseDTO getMedicineById(Long id);

    /**
     * Find medicine by exact name
     * @param name Medicine name
     * @return MedicineResponseDTO if found
     * @throws com.medid.exception.ResourceNotFoundException if medicine not found
     */
    MedicineResponseDTO getMedicineByName(String name);

    /**
     * Create a new medicine
     * @param medicineRequestDTO Medicine creation request
     * @return MedicineResponseDTO containing created medicine information
     * @throws com.medid.exception.InvalidRequestException if request data is invalid
     */
    MedicineResponseDTO createMedicine(MedicineRequestDTO medicineRequestDTO);

    /**
     * Update existing medicine
     * @param id Medicine ID
     * @param medicineRequestDTO Updated medicine data
     * @return MedicineResponseDTO containing updated medicine information
     * @throws com.medid.exception.ResourceNotFoundException if medicine not found
     */
    MedicineResponseDTO updateMedicine(Long id, MedicineRequestDTO medicineRequestDTO);

    /**
     * Find all medicines expiring on or before specified date
     * @param expiryDate Expiry date threshold
     * @return List of expiring medicines
     */
    List<MedicineResponseDTO> getExpiringMedicines(LocalDate expiryDate);

    /**
     * Find all medicines with low stock
     * @param minimumStock Stock threshold
     * @return List of low stock medicines
     */
    List<MedicineResponseDTO> getLowStockMedicines(Integer minimumStock);

    /**
     * Search medicines by name pattern (case-insensitive)
     * @param namePattern Search pattern
     * @return List of matching medicines
     */
    List<MedicineResponseDTO> searchMedicines(String namePattern);

    /**
     * Delete a medicine by ID
     * @param id Medicine ID
     * @throws com.medid.exception.ResourceNotFoundException if medicine not found
     */
    void deleteMedicine(Long id);
}
