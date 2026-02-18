package com.medid.service;

import com.medid.dto.PrescriptionItemRequestDTO;
import com.medid.dto.PrescriptionItemResponseDTO;
import com.medid.entity.Medicine;
import com.medid.entity.Prescription;
import com.medid.entity.PrescriptionItem;
import com.medid.exception.InvalidRequestException;
import com.medid.exception.ResourceNotFoundException;
import com.medid.repository.MedicineRepository;
import com.medid.repository.PrescriptionItemRepository;
import com.medid.repository.PrescriptionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of prescription item service.
 * Manages individual medicine items within prescriptions.
 */
@Slf4j
@Service
@Transactional
public class PrescriptionItemService implements IPrescriptionItemService {

    @Autowired
    private PrescriptionItemRepository prescriptionItemRepository;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private MedicineRepository medicineRepository;

    @Override
    public PrescriptionItemResponseDTO getPrescriptionItemById(Long id) {
        log.debug("Fetching prescription item with ID: {}", id);
        if (id == null || id <= 0) {
            throw new InvalidRequestException("Prescription item ID must be a positive number");
        }

        return prescriptionItemRepository.findById(id)
                .map(this::convertToResponse)
                .orElseThrow(() -> {
                    log.warn("Prescription item not found with ID: {}", id);
                    return new ResourceNotFoundException("Prescription item not found with ID: " + id);
                });
    }

    @Override
    public PrescriptionItemResponseDTO createPrescriptionItem(PrescriptionItemRequestDTO prescriptionItemRequestDTO) {
        log.info("Creating new prescription item for prescription: {}", prescriptionItemRequestDTO.getPrescriptionId());
        try {
            validatePrescriptionItemRequest(prescriptionItemRequestDTO);

            Prescription prescription = prescriptionRepository.findById(prescriptionItemRequestDTO.getPrescriptionId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Prescription not found with ID: " + prescriptionItemRequestDTO.getPrescriptionId()));

            Medicine medicine = medicineRepository.findById(prescriptionItemRequestDTO.getMedicineId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Medicine not found with ID: " + prescriptionItemRequestDTO.getMedicineId()));

            PrescriptionItem prescriptionItem = new PrescriptionItem();
            prescriptionItem.setPrescription(prescription);
            prescriptionItem.setMedicine(medicine);
            prescriptionItem.setQuantityRequired(prescriptionItemRequestDTO.getQuantityRequired());

            PrescriptionItem savedItem = prescriptionItemRepository.save(prescriptionItem);
            log.info("Prescription item created successfully with ID: {}", savedItem.getId());
            
            return convertToResponse(savedItem);
        } catch (InvalidRequestException | ResourceNotFoundException ex) {
            log.warn("Error creating prescription item: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error creating prescription item: {}", ex.getMessage(), ex);
            throw new InvalidRequestException("Failed to create prescription item: " + ex.getMessage(), ex);
        }
    }

    @Override
    public PrescriptionItemResponseDTO updatePrescriptionItem(Long id, PrescriptionItemRequestDTO prescriptionItemRequestDTO) {
        log.info("Updating prescription item with ID: {}", id);
        try {
            if (id == null || id <= 0) {
                throw new InvalidRequestException("Prescription item ID must be a positive number");
            }

            validatePrescriptionItemRequest(prescriptionItemRequestDTO);

            PrescriptionItem prescriptionItem = prescriptionItemRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Prescription item not found with ID: " + id));

            Prescription prescription = prescriptionRepository.findById(prescriptionItemRequestDTO.getPrescriptionId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Prescription not found with ID: " + prescriptionItemRequestDTO.getPrescriptionId()));

            Medicine medicine = medicineRepository.findById(prescriptionItemRequestDTO.getMedicineId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Medicine not found with ID: " + prescriptionItemRequestDTO.getMedicineId()));

            prescriptionItem.setPrescription(prescription);
            prescriptionItem.setMedicine(medicine);
            prescriptionItem.setQuantityRequired(prescriptionItemRequestDTO.getQuantityRequired());

            PrescriptionItem updatedItem = prescriptionItemRepository.save(prescriptionItem);
            log.info("Prescription item updated successfully with ID: {}", id);
            
            return convertToResponse(updatedItem);
        } catch (InvalidRequestException | ResourceNotFoundException ex) {
            log.warn("Error updating prescription item: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error updating prescription item: {}", ex.getMessage(), ex);
            throw new InvalidRequestException("Failed to update prescription item: " + ex.getMessage(), ex);
        }
    }

    @Override
    public List<PrescriptionItemResponseDTO> getItemsByPrescription(Long prescriptionId) {
        log.debug("Fetching items for prescription: {}", prescriptionId);
        if (prescriptionId == null || prescriptionId <= 0) {
            throw new InvalidRequestException("Prescription ID must be a positive number");
        }

        return prescriptionItemRepository.findByPrescriptionIdOrderByIdAsc(prescriptionId)
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    @Override
    public List<PrescriptionItemResponseDTO> getItemsByMedicine(Long medicineId) {
        log.debug("Fetching items for medicine: {}", medicineId);
        if (medicineId == null || medicineId <= 0) {
            throw new InvalidRequestException("Medicine ID must be a positive number");
        }

        return prescriptionItemRepository.findByMedicineIdOrderByCreatedAtDesc(medicineId)
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    @Override
    public List<PrescriptionItemResponseDTO> getPendingItemsByMedicine(Long medicineId) {
        log.debug("Fetching pending items for medicine: {}", medicineId);
        if (medicineId == null || medicineId <= 0) {
            throw new InvalidRequestException("Medicine ID must be a positive number");
        }

        return prescriptionItemRepository.findByMedicineIdAndPrescriptionStatus(medicineId, "PENDING")
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    @Override
    public void deletePrescriptionItem(Long id) {
        log.info("Deleting prescription item with ID: {}", id);
        if (id == null || id <= 0) {
            throw new InvalidRequestException("Prescription item ID must be a positive number");
        }

        PrescriptionItem prescriptionItem = prescriptionItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription item not found with ID: " + id));

        prescriptionItemRepository.delete(prescriptionItem);
        log.info("Prescription item deleted successfully with ID: {}", id);
    }

    /**
     * Validate prescription item request DTO
     */
    private void validatePrescriptionItemRequest(PrescriptionItemRequestDTO prescriptionItemRequestDTO) {
        if (prescriptionItemRequestDTO == null) {
            throw new InvalidRequestException("Prescription item request cannot be null");
        }

        if (prescriptionItemRequestDTO.getPrescriptionId() == null || prescriptionItemRequestDTO.getPrescriptionId() <= 0) {
            throw new InvalidRequestException("Prescription ID must be a positive number");
        }

        if (prescriptionItemRequestDTO.getMedicineId() == null || prescriptionItemRequestDTO.getMedicineId() <= 0) {
            throw new InvalidRequestException("Medicine ID must be a positive number");
        }

        if (prescriptionItemRequestDTO.getQuantityRequired() == null || prescriptionItemRequestDTO.getQuantityRequired() <= 0) {
            throw new InvalidRequestException("Quantity required must be a positive number");
        }
    }

    /**
     * Convert PrescriptionItem entity to PrescriptionItemResponseDTO
     */
    private PrescriptionItemResponseDTO convertToResponse(PrescriptionItem prescriptionItem) {
        return new PrescriptionItemResponseDTO(
                prescriptionItem.getId(),
                prescriptionItem.getPrescription().getId(),
                prescriptionItem.getMedicine().getId(),
                prescriptionItem.getMedicine().getName(),
                prescriptionItem.getQuantityRequired()
        );
    }
}
