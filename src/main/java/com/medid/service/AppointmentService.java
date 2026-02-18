package com.medid.service;

import com.medid.dto.AppointmentRequestDTO;
import com.medid.dto.AppointmentResponseDTO;
import com.medid.entity.Appointment;
import com.medid.entity.Patient;
import com.medid.exception.InvalidRequestException;
import com.medid.exception.ResourceNotFoundException;
import com.medid.repository.AppointmentRepository;
import com.medid.repository.PatientRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementation of appointment service.
 * Manages appointment scheduling and validation.
 */
@Slf4j
@Service
@Transactional
public class AppointmentService implements IAppointmentService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Override
    public AppointmentResponseDTO getAppointmentById(Long id) {
        log.debug("Fetching appointment with ID: {}", id);
        if (id == null || id <= 0) {
            throw new InvalidRequestException("Appointment ID must be a positive number");
        }

        return appointmentRepository.findById(id)
                .map(this::convertToResponse)
                .orElseThrow(() -> {
                    log.warn("Appointment not found with ID: {}", id);
                    return new ResourceNotFoundException("Appointment not found with ID: " + id);
                });
    }

    @Override
    public AppointmentResponseDTO createAppointment(AppointmentRequestDTO appointmentRequestDTO) {
        log.info("Creating new appointment for doctor: {}", appointmentRequestDTO.getDoctorId());
        try {
            validateAppointmentRequest(appointmentRequestDTO);

            Patient patient = patientRepository.findById(appointmentRequestDTO.getPatientId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Patient not found with ID: " + appointmentRequestDTO.getPatientId()));

            Appointment appointment = new Appointment();
            appointment.setPatient(patient);
            appointment.setDoctorId(appointmentRequestDTO.getDoctorId());
            appointment.setSlotTimestamp(appointmentRequestDTO.getSlotTimestamp());
            appointment.setStatus(appointmentRequestDTO.getStatus());

            Appointment savedAppointment = appointmentRepository.save(appointment);
            log.info("Appointment created successfully with ID: {}", savedAppointment.getId());
            
            return convertToResponse(savedAppointment);
        } catch (InvalidRequestException | ResourceNotFoundException ex) {
            log.warn("Error creating appointment: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error creating appointment: {}", ex.getMessage(), ex);
            throw new InvalidRequestException("Failed to create appointment: " + ex.getMessage(), ex);
        }
    }

    @Override
    public AppointmentResponseDTO updateAppointment(Long id, AppointmentRequestDTO appointmentRequestDTO) {
        log.info("Updating appointment with ID: {}", id);
        try {
            if (id == null || id <= 0) {
                throw new InvalidRequestException("Appointment ID must be a positive number");
            }

            validateAppointmentRequest(appointmentRequestDTO);

            Appointment appointment = appointmentRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + id));

            Patient patient = patientRepository.findById(appointmentRequestDTO.getPatientId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Patient not found with ID: " + appointmentRequestDTO.getPatientId()));

            appointment.setPatient(patient);
            appointment.setDoctorId(appointmentRequestDTO.getDoctorId());
            appointment.setSlotTimestamp(appointmentRequestDTO.getSlotTimestamp());
            appointment.setStatus(appointmentRequestDTO.getStatus());

            Appointment updatedAppointment = appointmentRepository.save(appointment);
            log.info("Appointment updated successfully with ID: {}", id);
            
            return convertToResponse(updatedAppointment);
        } catch (InvalidRequestException | ResourceNotFoundException ex) {
            log.warn("Error updating appointment: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error updating appointment: {}", ex.getMessage(), ex);
            throw new InvalidRequestException("Failed to update appointment: " + ex.getMessage(), ex);
        }
    }

    @Override
    public List<AppointmentResponseDTO> getAppointmentsByPatient(Long patientId) {
        log.debug("Fetching appointments for patient: {}", patientId);
        if (patientId == null || patientId <= 0) {
            throw new InvalidRequestException("Patient ID must be a positive number");
        }

        return appointmentRepository.findByPatientIdOrderBySlotTimestampDesc(patientId)
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    @Override
    public List<AppointmentResponseDTO> getAppointmentsByDoctor(Long doctorId) {
        log.debug("Fetching appointments for doctor: {}", doctorId);
        if (doctorId == null || doctorId <= 0) {
            throw new InvalidRequestException("Doctor ID must be a positive number");
        }

        return appointmentRepository.findByDoctorIdOrderBySlotTimestampDesc(doctorId)
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    @Override
    public List<AppointmentResponseDTO> getAppointmentsBetween(LocalDateTime startTime, LocalDateTime endTime) {
        log.debug("Fetching appointments between {} and {}", startTime, endTime);
        if (startTime == null || endTime == null) {
            throw new InvalidRequestException("Start and end times cannot be null");
        }

        if (startTime.isAfter(endTime)) {
            throw new InvalidRequestException("Start time cannot be after end time");
        }

        return appointmentRepository.findBySlotTimestampBetweenOrderBySlotTimestampAsc(startTime, endTime)
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    @Override
    public List<AppointmentResponseDTO> getBookedAppointments(Long doctorId) {
        log.debug("Fetching booked appointments for doctor: {}", doctorId);
        if (doctorId == null || doctorId <= 0) {
            throw new InvalidRequestException("Doctor ID must be a positive number");
        }

        return appointmentRepository.findByDoctorIdAndStatusOrderBySlotTimestampAsc(doctorId, "BOOKED")
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    @Override
    public boolean isSlotBooked(Long doctorId, LocalDateTime slotTime) {
        log.debug("Checking if slot is booked for doctor {} at {}", doctorId, slotTime);
        if (doctorId == null || doctorId <= 0) {
            throw new InvalidRequestException("Doctor ID must be a positive number");
        }

        if (slotTime == null) {
            throw new InvalidRequestException("Slot time cannot be null");
        }

        return appointmentRepository.existsByDoctorIdAndSlotTimestampAndStatus(doctorId, slotTime, "BOOKED");
    }

    @Override
    public List<AppointmentResponseDTO> getAppointmentsByStatus(String status) {
        log.debug("Fetching appointments with status: {}", status);
        if (status == null || status.trim().isEmpty()) {
            throw new InvalidRequestException("Status cannot be empty");
        }

        return appointmentRepository.findByStatus(status.trim())
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    @Override
    public void deleteAppointment(Long id) {
        log.info("Deleting appointment with ID: {}", id);
        if (id == null || id <= 0) {
            throw new InvalidRequestException("Appointment ID must be a positive number");
        }

        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + id));

        appointmentRepository.delete(appointment);
        log.info("Appointment deleted successfully with ID: {}", id);
    }

    /**
     * Validate appointment request DTO
     */
    private void validateAppointmentRequest(AppointmentRequestDTO appointmentRequestDTO) {
        if (appointmentRequestDTO == null) {
            throw new InvalidRequestException("Appointment request cannot be null");
        }

        if (appointmentRequestDTO.getPatientId() == null || appointmentRequestDTO.getPatientId() <= 0) {
            throw new InvalidRequestException("Patient ID must be a positive number");
        }

        if (appointmentRequestDTO.getDoctorId() == null || appointmentRequestDTO.getDoctorId() <= 0) {
            throw new InvalidRequestException("Doctor ID must be a positive number");
        }

        if (appointmentRequestDTO.getSlotTimestamp() == null) {
            throw new InvalidRequestException("Slot timestamp is required");
        }

        if (appointmentRequestDTO.getSlotTimestamp().isBefore(LocalDateTime.now())) {
            throw new InvalidRequestException("Slot timestamp cannot be in the past");
        }

        if (appointmentRequestDTO.getStatus() == null || appointmentRequestDTO.getStatus().trim().isEmpty()) {
            throw new InvalidRequestException("Status is required");
        }
    }

    /**
     * Convert Appointment entity to AppointmentResponseDTO
     */
    private AppointmentResponseDTO convertToResponse(Appointment appointment) {
        return new AppointmentResponseDTO(
                appointment.getId(),
                appointment.getPatient().getId(),
                appointment.getDoctorId(),
                appointment.getSlotTimestamp(),
                appointment.getStatus()
        );
    }
}
