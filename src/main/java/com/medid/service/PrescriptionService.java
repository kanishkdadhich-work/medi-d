package com.medid.service;

import com.medid.dto.PrescriptionRequestDTO;
import com.medid.dto.PrescriptionResponseDTO;
import com.medid.entity.Appointment;
import com.medid.entity.Medicine;
import com.medid.entity.Prescription;
import com.medid.entity.PrescriptionItem;
import com.medid.exception.InvalidRequestException;
import com.medid.exception.ResourceNotFoundException;
import com.medid.repository.AppointmentRepository;
import com.medid.repository.MedicineRepository;
import com.medid.repository.PrescriptionItemRepository;
import com.medid.repository.PrescriptionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
public class PrescriptionService implements IPrescriptionService {

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PrescriptionItemRepository prescriptionItemRepository;

    @Autowired
    private MedicineRepository medicineRepository;

    @Override
    public PrescriptionResponseDTO getPrescriptionById(Long id) {
        log.debug("Fetching prescription with ID: {}", id);
        if (id == null || id <= 0) {
            throw new InvalidRequestException("Prescription ID must be a positive number");
        }

        return prescriptionRepository.findById(id)
                .map(this::convertToResponse)
                .orElseThrow(() -> {
                    log.warn("Prescription not found with ID: {}", id);
                    return new ResourceNotFoundException("Prescription not found with ID: " + id);
                });
    }

    @Override
    public PrescriptionResponseDTO getPrescriptionByAppointment(Long appointmentId) {
        log.debug("Fetching prescription for appointment: {}", appointmentId);
        if (appointmentId == null || appointmentId <= 0) {
            throw new InvalidRequestException("Appointment ID must be a positive number");
        }

        return prescriptionRepository.findByAppointmentId(appointmentId)
                .map(this::convertToResponse)
                .orElseThrow(() -> {
                    log.warn("Prescription not found for appointment: {}", appointmentId);
                    return new ResourceNotFoundException("Prescription not found for appointment: " + appointmentId);
                });
    }

    @Override
    public PrescriptionResponseDTO createPrescription(PrescriptionRequestDTO prescriptionRequestDTO) {
        log.info("Creating new prescription for appointment: {}", prescriptionRequestDTO.getAppointmentId());
        try {
            validatePrescriptionRequest(prescriptionRequestDTO);

            Appointment appointment = appointmentRepository.findById(prescriptionRequestDTO.getAppointmentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Appointment not found with ID: " + prescriptionRequestDTO.getAppointmentId()));

            Prescription prescription = new Prescription();
            prescription.setAppointment(appointment);
            prescription.setDiagnosis(prescriptionRequestDTO.getDiagnosis());
            prescription.setStatus(prescriptionRequestDTO.getStatus() != null ? prescriptionRequestDTO.getStatus() : "PENDING");

            Prescription savedPrescription = prescriptionRepository.save(prescription);
            log.info("Prescription created successfully with ID: {}", savedPrescription.getId());

            return convertToResponse(savedPrescription);
        } catch (InvalidRequestException | ResourceNotFoundException ex) {
            log.warn("Error creating prescription: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error creating prescription: {}", ex.getMessage(), ex);
            throw new InvalidRequestException("Failed to create prescription: " + ex.getMessage(), ex);
        }
    }

    @Override
    public PrescriptionResponseDTO updatePrescription(Long id, PrescriptionRequestDTO prescriptionRequestDTO) {
        log.info("Updating prescription with ID: {}", id);
        try {
            if (id == null || id <= 0) {
                throw new InvalidRequestException("Prescription ID must be a positive number");
            }

            validatePrescriptionRequest(prescriptionRequestDTO);

            Prescription prescription = prescriptionRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Prescription not found with ID: " + id));

            Appointment appointment = appointmentRepository.findById(prescriptionRequestDTO.getAppointmentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Appointment not found with ID: " + prescriptionRequestDTO.getAppointmentId()));

            prescription.setAppointment(appointment);
            prescription.setDiagnosis(prescriptionRequestDTO.getDiagnosis());
            prescription.setStatus(prescriptionRequestDTO.getStatus());

            Prescription updatedPrescription = prescriptionRepository.save(prescription);
            log.info("Prescription updated successfully with ID: {}", id);

            return convertToResponse(updatedPrescription);
        } catch (InvalidRequestException | ResourceNotFoundException ex) {
            log.warn("Error updating prescription: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error updating prescription: {}", ex.getMessage(), ex);
            throw new InvalidRequestException("Failed to update prescription: " + ex.getMessage(), ex);
        }
    }

    @Override
    public List<PrescriptionResponseDTO> getPrescriptionsByStatus(String status) {
        log.debug("Fetching prescriptions with status: {}", status);
        if (status == null || status.trim().isEmpty()) {
            throw new InvalidRequestException("Status cannot be empty");
        }

        return prescriptionRepository.findByStatus(status.trim())
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    @Override
    public List<PrescriptionResponseDTO> getPendingPrescriptions() {
        log.debug("Fetching all pending prescriptions");
        return prescriptionRepository.findByStatus("PENDING")
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    @Override
    public List<PrescriptionResponseDTO> getDispensedPrescriptions() {
        log.debug("Fetching all dispensed prescriptions");
        return prescriptionRepository.findByStatus("DISPENSED")
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    @Override
    public List<PrescriptionResponseDTO> getPrescriptionsByPatient(Long patientId) {
        log.debug("Fetching prescriptions for patient: {}", patientId);
        if (patientId == null || patientId <= 0) {
            throw new InvalidRequestException("Patient ID must be a positive number");
        }

        return prescriptionRepository.findByAppointmentPatientIdOrderByCreatedAtDesc(patientId)
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    @Override
    public Long countPendingByPatient(Long patientId) {
        log.debug("Counting pending prescriptions for patient: {}", patientId);
        if (patientId == null || patientId <= 0) {
            throw new InvalidRequestException("Patient ID must be a positive number");
        }

        return prescriptionRepository.countByAppointmentPatientIdAndStatus(patientId, "PENDING");
    }

    @Override
    public void deletePrescription(Long id) {
        log.info("Deleting prescription with ID: {}", id);
        if (id == null || id <= 0) {
            throw new InvalidRequestException("Prescription ID must be a positive number");
        }

        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found with ID: " + id));

        prescriptionRepository.delete(prescription);
        log.info("Prescription deleted successfully with ID: {}", id);
    }

    @Override
    public PrescriptionResponseDTO dispensePrescription(Long id) {
        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found with id: " + id));

        List<PrescriptionItem> items = prescriptionItemRepository.findByPrescriptionIdOrderByIdAsc(id);
        if (items == null || items.isEmpty()) {
            throw new InvalidRequestException("No items to dispense for prescription id=" + id);
        }

        // Check stock availability
        for (PrescriptionItem item : items) {
            Medicine med = medicineRepository.findById(item.getMedicine().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Medicine not found with id: " + item.getMedicine().getId()));
            int required = item.getQuantityRequired() != null ? item.getQuantityRequired() : 0;
            int available = med.getStock() != null ? med.getStock() : 0;
            if (available < required) {
                log.warn("Insufficient stock for medicine {} needed={} available={}", med.getName(), required, available);
                throw new InvalidRequestException("Insufficient stock for: " + med.getName());
            }
        }

        // Deduct stock
        for (PrescriptionItem item : items) {
            Medicine med = medicineRepository.findById(item.getMedicine().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Medicine not found with id: " + item.getMedicine().getId()));
            int required = item.getQuantityRequired() != null ? item.getQuantityRequired() : 0;
            med.setStock(med.getStock() - required);
            medicineRepository.save(med);
            log.debug("Deducted {} from medicine id={} newStock={}", required, med.getId(), med.getStock());
        }

        prescription.setStatus("DISPENSED");
        Prescription saved = prescriptionRepository.save(prescription);
        log.info("Dispensed prescription id={}", saved.getId());
        return convertToResponse(saved);
    }

    /**
     * Validate prescription request DTO
     */
    private void validatePrescriptionRequest(PrescriptionRequestDTO prescriptionRequestDTO) {
        if (prescriptionRequestDTO == null) {
            throw new InvalidRequestException("Prescription request cannot be null");
        }

        if (prescriptionRequestDTO.getAppointmentId() == null || prescriptionRequestDTO.getAppointmentId() <= 0) {
            throw new InvalidRequestException("Appointment ID must be a positive number");
        }

        if (prescriptionRequestDTO.getDiagnosis() == null || prescriptionRequestDTO.getDiagnosis().trim().isEmpty()) {
            throw new InvalidRequestException("Diagnosis is required");
        }

        if (prescriptionRequestDTO.getStatus() == null || prescriptionRequestDTO.getStatus().trim().isEmpty()) {
            throw new InvalidRequestException("Status is required");
        }
    }

    /**
     * Convert Prescription entity to PrescriptionResponseDTO
     */
    private PrescriptionResponseDTO convertToResponse(Prescription prescription) {
        return new PrescriptionResponseDTO(
                prescription.getId(),
                prescription.getAppointment() != null ? prescription.getAppointment().getId() : null,
                prescription.getDiagnosis(),
                prescription.getStatus(),
                prescription.getCreatedAt()
        );
    }
}
