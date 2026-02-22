package com.medid.service;

import com.medid.entity.Appointment;
import com.medid.entity.Patient;
import com.medid.entity.User;
import com.medid.enums.Role;
import com.medid.repository.AppointmentRepository;
import com.medid.repository.PatientRepository;
import com.medid.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class AppointmentServiceImpl implements IAppointmentService {
    private static final Set<String> BLOCKING_STATUSES = Set.of("SCHEDULED", "BOOKED", "UNAVAILABLE");
    private static final Set<String> ACTIVE_PATIENT_STATUSES = Set.of("SCHEDULED", "BOOKED");

    @Autowired
    private AppointmentRepository appointmentRepo;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional // Ensures database consistency
    public Appointment bookAppointment(Long patientId, Long doctorId, LocalDateTime slot) {
        log.debug("Attempting to book appointment patientId={}, doctorId={}, slot={}", patientId, doctorId, slot);
        LocalDateTime dayStart = slot.toLocalDate().atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1).minusNanos(1);

        boolean patientAlreadyBookedWithDoctorSameDay =
                appointmentRepo.existsByPatient_PatientIdAndDoctorIdAndAppointmentTimeBetweenAndStatusIn(
                        patientId,
                        doctorId,
                        dayStart,
                        dayEnd,
                        ACTIVE_PATIENT_STATUSES
                );
        if (patientAlreadyBookedWithDoctorSameDay) {
            throw new RuntimeException("Patient already has an active appointment with this doctor on the selected day.");
        }

        // 1. Fetch the Patient entity
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found with ID: " + patientId));

        // 2. Slot Validation + historical-row reuse
        List<Appointment> sameSlot = appointmentRepo.findByDoctorIdAndAppointmentTime(doctorId, slot);
        Appointment reusable = null;
        for (Appointment existing : sameSlot) {
            String status = existing.getStatus() == null ? "" : existing.getStatus().toUpperCase();
            if (BLOCKING_STATUSES.contains(status)) {
                log.debug("Booking blocked due to slot conflict doctorId={}, slot={}, appointmentId={}", doctorId, slot, existing.getAppointmentId());
                throw new RuntimeException("Doctor is already booked for this time slot!");
            }
            if (reusable == null && (status.equals("CANCELLED") || status.equals("COMPLETED"))) {
                reusable = existing;
            }
        }

        Appointment appointment = reusable != null ? reusable : new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctorId(doctorId);
        appointment.setAppointmentTime(slot);
        appointment.setStatus("SCHEDULED");

        Appointment saved = appointmentRepo.save(appointment);
        log.debug("Appointment booked successfully appointmentId={}", saved.getAppointmentId());
        return saved;
    }

    @Override
    @Transactional
    public Appointment bookAppointment(Long patientId, String doctorRef, LocalDateTime slot) {
        Long resolvedDoctorId = resolveDoctorReference(doctorRef);
        log.debug("Resolved doctorRef={} to doctorId={}", doctorRef, resolvedDoctorId);
        return bookAppointment(patientId, resolvedDoctorId, slot);
    }

    @Override
    public void cancelAppointment(Long appointmentId) {
        log.debug("Cancelling appointment appointmentId={}", appointmentId);
        Appointment appt = appointmentRepo.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));
        appt.setStatus("CANCELLED");
        appointmentRepo.save(appt);
    }



    @Override
    public List<Appointment> getAppointmentsByDoctor(Long doctorId) {
        // Fetches all appointments for a specific doctor that aren't cancelled
        List<Appointment> appointments = appointmentRepo.findByDoctorIdAndStatusNot(doctorId, "CANCELLED");
        log.debug("Fetched {} appointments for doctorId={}", appointments.size(), doctorId);
        return appointments;
    }

    @Override
    @Transactional
    public void updateStatus(Long id, String status) {
        log.debug("Updating appointment status appointmentId={}, requestedStatus={}", id, status);
        Appointment appointment = appointmentRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        String normalized = status == null ? "" : status.trim().toUpperCase();
        if (!List.of("SCHEDULED", "BOOKED", "COMPLETED", "CANCELLED", "UNAVAILABLE").contains(normalized)) {
            throw new IllegalArgumentException("Invalid appointment status: " + status);
        }

        appointment.setStatus(normalized);
        appointmentRepo.save(appointment);
        log.debug("Appointment status updated appointmentId={}, status={}", id, normalized);
    }

    @Override
    public Long resolveDoctorReference(String doctorRef) {
        if (doctorRef == null || doctorRef.isBlank()) {
            throw new IllegalArgumentException("Doctor identifier is required.");
        }

        String trimmed = doctorRef.trim();
        if (trimmed.matches("\\d+")) {
            log.debug("Doctor reference {} treated as numeric doctor id", doctorRef);
            return Long.parseLong(trimmed);
        }

        User user = userRepository.findByUsernameAndRole(trimmed, Role.DOCTOR)
                .orElseThrow(() -> new RuntimeException("Doctor not found with username: " + trimmed));

        Long resolved = user.getDoctorId() != null ? user.getDoctorId() : user.getId();
        log.debug("Doctor reference {} resolved via username to doctorId={}", doctorRef, resolved);
        return resolved;
    }

    @Override
    @Transactional
    public Appointment markUnavailable(Long doctorId, LocalDateTime slot) {
        log.debug("Marking slot unavailable doctorId={}, slot={}", doctorId, slot);
        boolean exists = appointmentRepo.existsByDoctorIdAndAppointmentTimeAndStatusIn(doctorId, slot, BLOCKING_STATUSES);
        if (exists) {
            throw new RuntimeException("Slot already occupied and cannot be marked unavailable.");
        }

        Appointment appointment = new Appointment();
        appointment.setDoctorId(doctorId);
        appointment.setAppointmentTime(slot);
        appointment.setStatus("UNAVAILABLE");
        appointment.setPatient(null);
        return appointmentRepo.save(appointment);
    }

    @Override
    @Transactional
    public void clearUnavailable(Long doctorId, LocalDateTime slot) {
        log.debug("Clearing unavailable slot doctorId={}, slot={}", doctorId, slot);
        List<Appointment> entries = appointmentRepo.findByDoctorIdAndAppointmentTimeAndStatus(doctorId, slot, "UNAVAILABLE");
        if (entries.isEmpty()) {
            return;
        }
        for (Appointment appointment : entries) {
            appointment.setStatus("CANCELLED");
            appointmentRepo.save(appointment);
        }
    }

}
