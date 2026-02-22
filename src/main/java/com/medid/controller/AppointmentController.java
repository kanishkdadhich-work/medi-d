package com.medid.controller;

import com.medid.dto.AppointmentViewDTO;
import com.medid.dto.DoctorRefDTO;
import com.medid.dto.PatientViewDTO;
import com.medid.entity.Appointment;
import com.medid.entity.User;
import com.medid.enums.Role;
import com.medid.repository.AppointmentRepository;
import com.medid.repository.UserRepository;
import com.medid.service.IAppointmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@Slf4j
public class AppointmentController {

    @Autowired
    private IAppointmentService appointmentService;

    @Autowired
    private UserRepository userRepository;

    private final AppointmentRepository appointmentRepo;

    public AppointmentController(AppointmentRepository appointmentRepo) {
        this.appointmentRepo = appointmentRepo;
    }

    @PostMapping("/book")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','ADMIN')")
    public ResponseEntity<Appointment> book(
            @RequestParam Long patientId,
            @RequestParam Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime slot) {
        log.debug("Booking appointment via /book for patientId={}, doctorId={}, slot={}", patientId, doctorId, slot);
        return ResponseEntity.ok(appointmentService.bookAppointment(patientId, doctorId, slot));
    }

    @PostMapping("/book-flex")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','ADMIN')")
    public ResponseEntity<Appointment> bookByDoctorRef(
            @RequestParam Long patientId,
            @RequestParam String doctor,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime slot) {
        log.debug("Booking appointment via /book-flex for patientId={}, doctorRef={}, slot={}", patientId, doctor, slot);
        return ResponseEntity.ok(appointmentService.bookAppointment(patientId, doctor, slot));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('DOCTOR','RECEPTIONIST','ADMIN')")
    public ResponseEntity<String> cancel(@PathVariable Long id) {
        log.debug("Cancelling appointment id={}", id);
        appointmentService.cancelAppointment(id);
        return ResponseEntity.ok("Appointment cancelled successfully.");
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('DOCTOR','RECEPTIONIST','ADMIN')")
    public ResponseEntity<String> updateAppointmentStatus(@PathVariable Long id, @RequestParam String status) {
        log.debug("Updating appointment status id={}, status={}", id, status);
        appointmentService.updateStatus(id, status);
        return ResponseEntity.ok("Appointment status updated successfully.");
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('RECEPTIONIST','ADMIN')")
    public ResponseEntity<List<AppointmentViewDTO>> getAllAppointments(Authentication authentication) {
        boolean includeMedical = hasMedicalAccess(authentication);
        log.debug("Fetching all appointments includeMedical={}", includeMedical);
        List<AppointmentViewDTO> result = appointmentRepo.findAll().stream()
                .map(a -> toViewDto(a, includeMedical))
                .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/doctor/today")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<List<AppointmentViewDTO>> getDoctorConsultations(@RequestParam Long doctorId) {
        log.debug("Fetching doctor consultations for doctorId={}", doctorId);
        List<AppointmentViewDTO> result = appointmentService.getAppointmentsByDoctor(doctorId).stream()
                .map(a -> toViewDto(a, true))
                .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/doctor/my")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<List<AppointmentViewDTO>> getMyConsultations(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Long doctorId = user.getDoctorId() != null ? user.getDoctorId() : user.getId();
        log.debug("Fetching authenticated doctor consultations username={}, doctorId={}", user.getUsername(), doctorId);
        List<AppointmentViewDTO> result = appointmentService.getAppointmentsByDoctor(doctorId).stream()
                .map(a -> toViewDto(a, true))
                .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/doctors")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','ADMIN')")
    public ResponseEntity<List<DoctorRefDTO>> getDoctors() {
        log.debug("Fetching doctor references for receptionist/admin");
        List<DoctorRefDTO> doctors = userRepository.findByRole(Role.DOCTOR).stream()
                .map(u -> new DoctorRefDTO(
                        u.getDoctorId() != null ? u.getDoctorId() : u.getId(),
                        u.getId(),
                        u.getUsername(),
                        u.getSpecialization()
                ))
                .toList();
        return ResponseEntity.ok(doctors);
    }

    @PostMapping("/doctor/unavailable")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<AppointmentViewDTO> markUnavailable(Authentication authentication,
                                                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime slot) {
        User user = (User) authentication.getPrincipal();
        Long doctorId = user.getDoctorId() != null ? user.getDoctorId() : user.getId();
        Appointment appointment = appointmentService.markUnavailable(doctorId, slot);
        return ResponseEntity.ok(toViewDto(appointment, true));
    }

    @DeleteMapping("/doctor/unavailable")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<String> clearUnavailable(Authentication authentication,
                                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime slot) {
        User user = (User) authentication.getPrincipal();
        Long doctorId = user.getDoctorId() != null ? user.getDoctorId() : user.getId();
        appointmentService.clearUnavailable(doctorId, slot);
        return ResponseEntity.ok("Unavailable slot cleared");
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public ResponseEntity<String> completeConsultation(@PathVariable Long id) {
        log.debug("Marking consultation complete for appointmentId={}", id);
        appointmentService.updateStatus(id, "COMPLETED");
        return ResponseEntity.ok("Consultation marked as completed. Ready for pharmacy.");
    }

    private boolean hasMedicalAccess(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            return false;
        }
        return user.getRole() == Role.DOCTOR || user.getRole() == Role.ADMIN;
    }

    private AppointmentViewDTO toViewDto(Appointment appointment, boolean includeMedicalBlob) {
        PatientViewDTO patient = null;
        if (appointment.getPatient() != null) {
            patient = new PatientViewDTO(
                    appointment.getPatient().getPatientId(),
                    appointment.getPatient().getFullName(),
                    appointment.getPatient().getPhoneNumber(),
                    appointment.getPatient().getGender(),
                    includeMedicalBlob ? appointment.getPatient().getMedicalHistoryBlob() : null,
                    appointment.getPatient().getCreatedAt()
            );
        }

        return new AppointmentViewDTO(
                appointment.getAppointmentId(),
                appointment.getDoctorId(),
                appointment.getAppointmentTime(),
                appointment.getStatus(),
                patient
        );
    }
}
