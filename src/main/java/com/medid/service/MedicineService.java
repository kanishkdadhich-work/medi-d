package com.medid.service;

import com.medid.dto.MedicineRequestDTO;
import com.medid.dto.MedicineResponseDTO;
import com.medid.entity.Medicine;
import com.medid.exception.InvalidRequestException;
import com.medid.exception.ResourceNotFoundException;
import com.medid.repository.MedicineRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Implementation of medicine service.
 * Handles inventory management, stock tracking, and expiry monitoring.
 */
@Slf4j
@Service
@Transactional
public class MedicineService implements IMedicineService {

    @Autowired
    private MedicineRepository medicineRepository;

    @Override
    public MedicineResponseDTO getMedicineById(Long id) {
        log.debug("Fetching medicine with ID: {}", id);
        if (id == null || id <= 0) {
            throw new InvalidRequestException("Medicine ID must be a positive number");
        }

        return medicineRepository.findById(id)
                .map(this::convertToResponse)
                .orElseThrow(() -> {
                    log.warn("Medicine not found with ID: {}", id);
                    return new ResourceNotFoundException("Medicine not found with ID: " + id);
                });
    }

    @Override
    public MedicineResponseDTO getMedicineByName(String name) {
        log.debug("Fetching medicine with name: {}", name);
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidRequestException("Medicine name cannot be empty");
        }

        return medicineRepository.findByName(name.trim())
                .map(this::convertToResponse)
                .orElseThrow(() -> {
                    log.warn("Medicine not found with name: {}", name);
                    return new ResourceNotFoundException("Medicine not found with name: " + name);
                });
    }

    @Override
    public MedicineResponseDTO createMedicine(MedicineRequestDTO medicineRequestDTO) {
        log.info("Creating new medicine: {}", medicineRequestDTO.getName());
        try {
            validateMedicineRequest(medicineRequestDTO);

            Medicine medicine = new Medicine();
            medicine.setName(medicineRequestDTO.getName().trim());
            medicine.setStock(medicineRequestDTO.getStock());
            medicine.setExpiryDate(medicineRequestDTO.getExpiryDate());

            Medicine savedMedicine = medicineRepository.save(medicine);
            log.info("Medicine created successfully with ID: {}", savedMedicine.getId());
            
            return convertToResponse(savedMedicine);
        } catch (InvalidRequestException ex) {
            log.warn("Invalid request for medicine creation: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Error creating medicine: {}", ex.getMessage(), ex);
            throw new InvalidRequestException("Failed to create medicine: " + ex.getMessage(), ex);
        }
    }

    @Override
    public MedicineResponseDTO updateMedicine(Long id, MedicineRequestDTO medicineRequestDTO) {
        log.info("Updating medicine with ID: {}", id);
        try {
            if (id == null || id <= 0) {
                throw new InvalidRequestException("Medicine ID must be a positive number");
            }

            validateMedicineRequest(medicineRequestDTO);

            Medicine medicine = medicineRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Medicine not found with ID: " + id));

            medicine.setName(medicineRequestDTO.getName().trim());
            medicine.setStock(medicineRequestDTO.getStock());
            medicine.setExpiryDate(medicineRequestDTO.getExpiryDate());

            Medicine updatedMedicine = medicineRepository.save(medicine);
            log.info("Medicine updated successfully with ID: {}", id);
            
            return convertToResponse(updatedMedicine);
        } catch (InvalidRequestException | ResourceNotFoundException ex) {
            log.warn("Error updating medicine: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error updating medicine: {}", ex.getMessage(), ex);
            throw new InvalidRequestException("Failed to update medicine: " + ex.getMessage(), ex);
        }
    }

    @Override
    public List<MedicineResponseDTO> getExpiringMedicines(LocalDate expiryDate) {
        log.debug("Fetching medicines expiring on or before: {}", expiryDate);
        if (expiryDate == null) {
            throw new InvalidRequestException("Expiry date cannot be null");
        }

        return medicineRepository.findByExpiryDateLessThanEqualOrderByExpiryDateAsc(expiryDate)
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    @Override
    public List<MedicineResponseDTO> getLowStockMedicines(Integer minimumStock) {
        log.debug("Fetching medicines with stock <= {}", minimumStock);
        if (minimumStock == null || minimumStock < 0) {
            throw new InvalidRequestException("Minimum stock must be a non-negative number");
        }

        return medicineRepository.findByStockLessThanEqualOrderByStockAsc(minimumStock)
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    @Override
    public List<MedicineResponseDTO> searchMedicines(String namePattern) {
        log.debug("Searching medicines with pattern: {}", namePattern);
        if (namePattern == null || namePattern.trim().isEmpty()) {
            throw new InvalidRequestException("Search pattern cannot be empty");
        }

        return medicineRepository.findByNameContainingIgnoreCase(namePattern.trim())
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    @Override
    public void deleteMedicine(Long id) {
        log.info("Deleting medicine with ID: {}", id);
        if (id == null || id <= 0) {
            throw new InvalidRequestException("Medicine ID must be a positive number");
        }

        Medicine medicine = medicineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medicine not found with ID: " + id));

        medicineRepository.delete(medicine);
        log.info("Medicine deleted successfully with ID: {}", id);
    }

    /**
     * Validate medicine request DTO
     */
    private void validateMedicineRequest(MedicineRequestDTO medicineRequestDTO) {
        if (medicineRequestDTO == null) {
            throw new InvalidRequestException("Medicine request cannot be null");
        }

        if (medicineRequestDTO.getName() == null || medicineRequestDTO.getName().trim().isEmpty()) {
            throw new InvalidRequestException("Medicine name cannot be empty");
        }

        if (medicineRequestDTO.getStock() == null || medicineRequestDTO.getStock() < 0) {
            throw new InvalidRequestException("Stock must be a non-negative number");
        }

        if (medicineRequestDTO.getExpiryDate() == null) {
            throw new InvalidRequestException("Expiry date is required");
        }

        if (medicineRequestDTO.getExpiryDate().isBefore(LocalDate.now())) {
            throw new InvalidRequestException("Expiry date cannot be in the past");
        }
    }

    /**
     * Convert Medicine entity to MedicineResponseDTO
     */
    private MedicineResponseDTO convertToResponse(Medicine medicine) {
        return new MedicineResponseDTO(
                medicine.getId(),
                medicine.getName(),
                medicine.getStock(),
                medicine.getExpiryDate(),
                medicine.getVersion()
        );
    }
}
