package com.medid.controller;

import com.medid.dto.AppointmentRequestDTO;
import org.springframework.security.access.prepost.PreAuthorize;
import com.medid.dto.AppointmentResponseDTO;
import com.medid.service.IAppointmentService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    @Autowired
    private IAppointmentService appointmentService;

    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponseDTO> getAppointmentById(@PathVariable Long id) {
        log.info("Fetching appointment with ID: {}", id);
        AppointmentResponseDTO appointment = appointmentService.getAppointmentById(id);
        return ResponseEntity.ok(appointment);
    }

    @PostMapping
    public ResponseEntity<AppointmentResponseDTO> createAppointment(@Valid @RequestBody AppointmentRequestDTO appointmentRequestDTO) {
        log.info("Creating new appointment for patient: {}", appointmentRequestDTO.getPatientId());
        AppointmentResponseDTO createdAppointment = appointmentService.createAppointment(appointmentRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAppointment);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AppointmentResponseDTO> updateAppointment(@PathVariable Long id, @Valid @RequestBody AppointmentRequestDTO appointmentRequestDTO) {
        log.info("Updating appointment with ID: {}", id);
        AppointmentResponseDTO updatedAppointment = appointmentService.updateAppointment(id, appointmentRequestDTO);
        return ResponseEntity.ok(updatedAppointment);
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('RECEPTIONIST')")
    public ResponseEntity<List<AppointmentResponseDTO>> getAppointmentsByPatient(@PathVariable Long patientId) {
        log.info("Fetching appointments for patient: {}", patientId);
        List<AppointmentResponseDTO> appointments = appointmentService.getAppointmentsByPatient(patientId);
        return ResponseEntity.ok(appointments);
    }

    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<List<AppointmentResponseDTO>> getAppointmentsByDoctor(@PathVariable Long doctorId) {
        log.info("Fetching appointments for doctor: {}", doctorId);
        List<AppointmentResponseDTO> appointments = appointmentService.getAppointmentsByDoctor(doctorId);
        return ResponseEntity.ok(appointments);
    }

    @GetMapping("/search/between")
    public ResponseEntity<List<AppointmentResponseDTO>> getAppointmentsBetween(
            @RequestParam LocalDateTime startTime, @RequestParam LocalDateTime endTime) {
        log.info("Fetching appointments between {} and {}", startTime, endTime);
        List<AppointmentResponseDTO> appointments = appointmentService.getAppointmentsBetween(startTime, endTime);
        return ResponseEntity.ok(appointments);
    }

    @GetMapping("/booked/{doctorId}")
    public ResponseEntity<List<AppointmentResponseDTO>> getBookedAppointments(@PathVariable Long doctorId) {
        log.info("Fetching booked appointments for doctor: {}", doctorId);
        List<AppointmentResponseDTO> appointments = appointmentService.getBookedAppointments(doctorId);
        return ResponseEntity.ok(appointments);
    }

    @GetMapping("/check-slot")
    public ResponseEntity<Boolean> isSlotBooked(@RequestParam Long doctorId, @RequestParam LocalDateTime slotTime) {
        boolean isBooked = appointmentService.isSlotBooked(doctorId, slotTime);
        return ResponseEntity.ok(isBooked);
    }

    @GetMapping("/check-available")
    public ResponseEntity<Boolean> isSlotAvailable(@RequestParam Long doctorId, @RequestParam LocalDateTime slotTime) {
        boolean available = appointmentService.isSlotAvailable(doctorId, slotTime);
        return ResponseEntity.ok(available);
    }

    @PostMapping("/mark-unavailable")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<Void> markSlotUnavailable(@RequestParam Long doctorId, @RequestParam LocalDateTime slotTime) {
        appointmentService.markSlotUnavailable(doctorId, slotTime);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/by-status/{status}")
    public ResponseEntity<List<AppointmentResponseDTO>> getAppointmentsByStatus(@PathVariable String status) {
        log.info("Fetching appointments with status: {}", status);
        List<AppointmentResponseDTO> appointments = appointmentService.getAppointmentsByStatus(status);
        return ResponseEntity.ok(appointments);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAppointment(@PathVariable Long id) {
        log.info("Deleting appointment with ID: {}", id);
        appointmentService.deleteAppointment(id);
        return ResponseEntity.noContent().build();
    }
}
