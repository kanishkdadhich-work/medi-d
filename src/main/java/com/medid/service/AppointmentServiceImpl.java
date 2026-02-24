package com.medid.service;

import com.medid.entity.Appointment;
import com.medid.entity.Patient;
import com.medid.entity.User;
import com.medid.enums.Role;
import com.medid.exception.ConflictException;
import com.medid.exception.ResourceNotFoundException;
import com.medid.exception.ValidationException;
import com.medid.repository.AppointmentRepository;
import com.medid.repository.PatientRepository;
import com.medid.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Slf4j
public class AppointmentServiceImpl implements IAppointmentService {
    // These statuses make a slot unavailable for new bookings.
    private static final Set<String> BLOCKING_STATUSES = Set.of("SCHEDULED", "BOOKED", "UNAVAILABLE");
    // Patient cannot hold multiple active appointments with same doctor on same day.
    private static final Set<String> ACTIVE_PATIENT_STATUSES = Set.of("SCHEDULED", "BOOKED");

    @Autowired
    private AppointmentRepository appointmentRepo;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    @Transactional // Ensures database consistency
    public Appointment bookAppointment(Long patientId, Long doctorId, LocalDateTime slot) {
        log.debug("Attempting to book appointment patientId={}, doctorId={}, slot={}", patientId, doctorId, slot);
        // Prevent back-dated booking entries.
        if (slot == null) {
            throw new ValidationException("Appointment slot is required.");
        }
        if (slot.isBefore(LocalDateTime.now())) {
            throw new ValidationException("Appointment slot must be in the future.");
        }
        // Backend guard: enforce only 30-minute aligned slots.
        if (!(slot.getMinute() == 0 || slot.getMinute() == 30) || slot.getSecond() != 0 || slot.getNano() != 0) {
            throw new ValidationException("Appointment slot must be on a 30-minute boundary (HH:00 or HH:30).");
        }

        User doctor = resolveDoctorByAppointmentId(doctorId);
        Long resolvedDoctorId = ensureDoctorProfileLinked(doctor);
        validateDoctorScheduleForSlot(doctor, slot);

        LocalDateTime dayStart = slot.toLocalDate().atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1).minusNanos(1);

        // Daily uniqueness rule for patient-doctor pair.
        boolean patientAlreadyBookedWithDoctorSameDay =
                appointmentRepo.existsByPatient_PatientIdAndDoctorIdAndAppointmentTimeBetweenAndStatusIn(
                        patientId,
                        resolvedDoctorId,
                        dayStart,
                        dayEnd,
                        ACTIVE_PATIENT_STATUSES
                );
        if (patientAlreadyBookedWithDoctorSameDay) {
            throw new ConflictException("Patient already has an active appointment with this doctor on the selected day.");
        }

        // 1. Fetch the Patient entity
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + patientId));

        // 2. Slot Validation + historical-row reuse
        // Slot-level conflict rule plus historical row reuse for cancelled/completed records.
        List<Appointment> sameSlot = appointmentRepo.findByDoctorIdAndAppointmentTime(resolvedDoctorId, slot);
        Appointment reusable = null;
        for (Appointment existing : sameSlot) {
            String status = existing.getStatus() == null ? "" : existing.getStatus().toUpperCase();
            if (BLOCKING_STATUSES.contains(status)) {
                log.debug("Booking blocked due to slot conflict doctorId={}, slot={}, appointmentId={}", resolvedDoctorId, slot, existing.getAppointmentId());
                throw new ConflictException("Doctor is already booked for this time slot!");
            }
            if (reusable == null && (status.equals("CANCELLED") || status.equals("COMPLETED"))) {
                reusable = existing;
            }
        }

        Appointment appointment = reusable != null ? reusable : new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctorId(resolvedDoctorId);
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
        log.debug("Cancelling appointment appointmentId={} via transition rules", appointmentId);
        updateStatus(appointmentId, "CANCELLED");
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

        String current = appointment.getStatus() == null ? "" : appointment.getStatus().trim().toUpperCase();
        String normalized = status == null ? "" : status.trim().toUpperCase();
        if ("BOOKED".equals(normalized)) normalized = "SCHEDULED";
        if ("BOOKED".equals(current)) current = "SCHEDULED";
        if (!List.of("SCHEDULED", "COMPLETED", "CANCELLED", "UNAVAILABLE").contains(normalized)) {
            throw new ValidationException("Invalid appointment status: " + status);
        }

        if (!isAllowedTransition(current, normalized)) {
            throw new ConflictException("Invalid status transition from " + current + " to " + normalized + ".");
        }

        appointment.setStatus(normalized);
        appointmentRepo.save(appointment);
        log.debug("Appointment status updated appointmentId={}, status={}", id, normalized);
    }

    @Override
    public Long resolveDoctorReference(String doctorRef) {
        if (doctorRef == null || doctorRef.isBlank()) {
            throw new ValidationException("Doctor identifier is required.");
        }

        String trimmed = doctorRef.trim();
        if (trimmed.matches("\\d+")) {
            log.debug("Doctor reference {} treated as numeric doctor id", doctorRef);
            Long numeric = Long.parseLong(trimmed);
            User doctor = resolveDoctorByAppointmentId(numeric);
            return ensureDoctorProfileLinked(doctor);
        }

        if (trimmed.toUpperCase(Locale.ROOT).startsWith("MEDID-")) {
            if (!trimmed.toUpperCase(Locale.ROOT).matches("^MEDID-[0-9]{2}$")) {
                throw new ValidationException("Doctor reference ID must match MEDID-XX format.");
            }
            User user = userRepository.findByDoctorRefCodeIgnoreCaseAndRole(trimmed, Role.DOCTOR)
                    .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with reference: " + trimmed));
            if (user.getEnabled() != null && !user.getEnabled()) {
                throw new ConflictException("Doctor account is disabled.");
            }
            return ensureDoctorProfileLinked(user);
        }

        User user = userRepository.findByUsernameAndRole(trimmed, Role.DOCTOR)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with username: " + trimmed));
        if (user.getEnabled() != null && !user.getEnabled()) {
            throw new ConflictException("Doctor account is disabled.");
        }

        Long resolved = ensureDoctorProfileLinked(user);
        log.debug("Doctor reference {} resolved via username to doctorId={}", doctorRef, resolved);
        return resolved;
    }

    @Override
    @Transactional
    public Appointment markUnavailable(Long doctorId, LocalDateTime slot) {
        log.debug("Marking slot unavailable doctorId={}, slot={}", doctorId, slot);
        User doctor = resolveDoctorByAppointmentId(doctorId);
        Long resolvedDoctorId = ensureDoctorProfileLinked(doctor);
        validateDoctorScheduleForSlot(doctor, slot);
        boolean exists = appointmentRepo.existsByDoctorIdAndAppointmentTimeAndStatusIn(resolvedDoctorId, slot, BLOCKING_STATUSES);
        if (exists) {
            throw new ConflictException("Slot already occupied and cannot be marked unavailable.");
        }

        Appointment appointment = new Appointment();
        appointment.setDoctorId(resolvedDoctorId);
        appointment.setAppointmentTime(slot);
        appointment.setStatus("UNAVAILABLE");
        appointment.setPatient(null);
        return appointmentRepo.save(appointment);
    }

    @Override
    @Transactional
    public void clearUnavailable(Long doctorId, LocalDateTime slot) {
        log.debug("Clearing unavailable slot doctorId={}, slot={}", doctorId, slot);
        User doctor = resolveDoctorByAppointmentId(doctorId);
        Long resolvedDoctorId = ensureDoctorProfileLinked(doctor);
        List<Appointment> entries = appointmentRepo.findByDoctorIdAndAppointmentTimeAndStatus(resolvedDoctorId, slot, "UNAVAILABLE");
        if (entries.isEmpty()) {
            return;
        }
        for (Appointment appointment : entries) {
            appointment.setStatus("CANCELLED");
            appointmentRepo.save(appointment);
        }
    }

    private User resolveDoctorByAppointmentId(Long doctorId) {
        User doctor = userRepository.findByDoctorIdAndRole(doctorId, Role.DOCTOR)
                .or(() -> userRepository.findByIdAndRole(doctorId, Role.DOCTOR))
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with identifier: " + doctorId));
        if (doctor.getEnabled() != null && !doctor.getEnabled()) {
            throw new ConflictException("Doctor account is disabled.");
        }
        return doctor;
    }

    private Long ensureDoctorProfileLinked(User doctor) {
        if (doctor.getDoctorId() != null) {
            return doctor.getDoctorId();
        }

        String doctorName = doctor.getUsername() == null ? "Doctor" : doctor.getUsername().trim();
        if (!doctorName.toLowerCase(Locale.ROOT).startsWith("dr.")) {
            doctorName = "Dr. " + doctorName;
        }

        Long newDoctorId = jdbcTemplate.queryForObject(
                "INSERT INTO doctors (name, specialization, is_available) VALUES (?, ?, true) RETURNING doctor_id",
                Long.class,
                doctorName,
                doctor.getSpecialization()
        );
        if (newDoctorId == null) {
            throw new RuntimeException("Unable to link doctor profile.");
        }

        doctor.setDoctorId(newDoctorId);
        userRepository.save(doctor);
        log.debug("Linked userId={} to doctors.doctor_id={}", doctor.getId(), newDoctorId);
        return newDoctorId;
    }

    private void validateDoctorScheduleForSlot(User doctor, LocalDateTime slot) {
        DayOfWeek day = slot.getDayOfWeek();
        boolean weekend = day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;

        String weekdayShift = doctor.getWeekdayShift() == null ? "" : doctor.getWeekdayShift().trim().toUpperCase(Locale.ROOT);
        String weekendShift = doctor.getWeekendShift() == null ? "" : doctor.getWeekendShift().trim().toUpperCase(Locale.ROOT);
        String activeShift = weekend ? weekendShift : weekdayShift;

        if (activeShift.isBlank()) {
            throw new ConflictException("Doctor is not available on selected day type.");
        }
        if (!Set.of("MORNING", "EVENING", "NIGHT").contains(activeShift)) {
            throw new ValidationException("Doctor shift is invalid. Must be MORNING, EVENING, or NIGHT.");
        }

        LocalTime time = slot.toLocalTime();
        boolean withinShift =
                ("MORNING".equals(activeShift) && !time.isBefore(LocalTime.of(6, 0)) && time.isBefore(LocalTime.of(14, 0)))
                        || ("EVENING".equals(activeShift) && !time.isBefore(LocalTime.of(14, 0)) && time.isBefore(LocalTime.of(22, 0)))
                        || ("NIGHT".equals(activeShift) && (!time.isBefore(LocalTime.of(22, 0)) || time.isBefore(LocalTime.of(6, 0))));

        if (!withinShift) {
            throw new ConflictException("Selected slot is outside doctor's configured shift window.");
        }
    }

    private boolean isAllowedTransition(String current, String target) {
        if (current == null || current.isBlank()) {
            return "SCHEDULED".equals(target) || "CANCELLED".equals(target);
        }
        if ("COMPLETED".equals(current)) {
            return "COMPLETED".equals(target);
        }
        if ("CANCELLED".equals(current)) {
            return "CANCELLED".equals(target);
        }
        if ("SCHEDULED".equals(current)) {
            return "SCHEDULED".equals(target) || "CANCELLED".equals(target) || "COMPLETED".equals(target);
        }
        if ("UNAVAILABLE".equals(current)) {
            return "UNAVAILABLE".equals(target) || "CANCELLED".equals(target);
        }
        return false;
    }

}
