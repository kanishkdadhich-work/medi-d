package com.medid.service;

import com.medid.dto.AppointmentRequestDTO;
import com.medid.dto.AppointmentResponseDTO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service interface for appointment-related operations.
 * Manages appointment scheduling, validation, and retrieval.
 */
public interface IAppointmentService {

    /**
     * Retrieve an appointment by ID
     * @param id Appointment ID
     * @return AppointmentResponseDTO containing appointment information
     * @throws com.medid.exception.ResourceNotFoundException if appointment not found
     */
    AppointmentResponseDTO getAppointmentById(Long id);

    /**
     * Create a new appointment
     * @param appointmentRequestDTO Appointment creation request
     * @return AppointmentResponseDTO containing created appointment information
     * @throws com.medid.exception.InvalidRequestException if request data is invalid
     * @throws com.medid.exception.ResourceNotFoundException if patient or doctor not found
     */
    AppointmentResponseDTO createAppointment(AppointmentRequestDTO appointmentRequestDTO);

    /**
     * Update an existing appointment
     * @param id Appointment ID
     * @param appointmentRequestDTO Updated appointment data
     * @return AppointmentResponseDTO containing updated appointment information
     * @throws com.medid.exception.ResourceNotFoundException if appointment not found
     */
    AppointmentResponseDTO updateAppointment(Long id, AppointmentRequestDTO appointmentRequestDTO);

    /**
     * Get all appointments for a specific patient
     * @param patientId Patient ID
     * @return List of patient's appointments (ordered by date descending)
     */
    List<AppointmentResponseDTO> getAppointmentsByPatient(Long patientId);

    /**
     * Get all appointments for a specific doctor
     * @param doctorId Doctor ID
     * @return List of doctor's appointments (ordered by date descending)
     */
    List<AppointmentResponseDTO> getAppointmentsByDoctor(Long doctorId);

    /**
     * Get appointments within a specific time range
     * @param startTime Start time
     * @param endTime End time
     * @return List of appointments in the time range
     */
    List<AppointmentResponseDTO> getAppointmentsBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Get booked appointments for a doctor
     * @param doctorId Doctor ID
     * @return List of booked appointments (ordered by time ascending)
     */
    List<AppointmentResponseDTO> getBookedAppointments(Long doctorId);

    /**
     * Check if a slot is available for a doctor
     * @param doctorId Doctor ID
     * @param slotTime Slot time
     * @return true if slot is booked, false if available
     */
    boolean isSlotBooked(Long doctorId, LocalDateTime slotTime);

    /**
     * Get appointments by status
     * @param status Appointment status (e.g., BOOKED, COMPLETED, CANCELLED)
     * @return List of appointments with the specified status
     */
    List<AppointmentResponseDTO> getAppointmentsByStatus(String status);

    /**
     * Delete an appointment by ID
     * @param id Appointment ID
     * @throws com.medid.exception.ResourceNotFoundException if appointment not found
     */
    void deleteAppointment(Long id);
}
