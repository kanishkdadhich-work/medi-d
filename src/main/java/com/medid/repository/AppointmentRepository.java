package com.medid.repository;

import com.medid.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Collection;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // Requirement 3.2: Slot Validation
    boolean existsByDoctorIdAndAppointmentTimeAndStatusIn(Long doctorId, LocalDateTime appointmentTime, Collection<String> statuses);

    // Requirement 3.4: Smart Filtering by Doctor
    List<Appointment> findByDoctorId(Long doctorId);

    // Requirement 3.4: Smart Filtering by Patient History
    List<Appointment> findByPatient_PatientId(Long patientId);

    // Requirement 3.4: Filter by Status
    List<Appointment> findByStatus(String status);

    List<Appointment> findByDoctorIdAndStatusNot(Long doctorId, String status);

    List<Appointment> findByDoctorIdAndAppointmentTimeAndStatus(Long doctorId, LocalDateTime appointmentTime, String status);

    List<Appointment> findByDoctorIdAndAppointmentTime(Long doctorId, LocalDateTime appointmentTime);

    boolean existsByPatient_PatientIdAndDoctorIdAndAppointmentTimeBetweenAndStatusIn(
            Long patientId,
            Long doctorId,
            LocalDateTime start,
            LocalDateTime end,
            Collection<String> statuses
    );
}
