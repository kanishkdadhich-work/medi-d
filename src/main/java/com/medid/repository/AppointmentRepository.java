package com.medid.repository;

import com.medid.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    
    /**
     * Find all appointments for a specific patient (ordered by slot timestamp descending)
     */
    List<Appointment> findByPatientIdOrderBySlotTimestampDesc(Long patientId);

    /**
     * Find all appointments for a specific doctor (ordered by slot timestamp descending)
     */
    List<Appointment> findByDoctorIdOrderBySlotTimestampDesc(Long doctorId);

    /**
     * Find appointments within a specific time range (ordered by slot timestamp ascending)
     */
    List<Appointment> findBySlotTimestampBetweenOrderBySlotTimestampAsc(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Find all booked appointments for a specific doctor (ordered by slot timestamp ascending)
     */
    List<Appointment> findByDoctorIdAndStatusOrderBySlotTimestampAsc(Long doctorId, String status);

    /**
     * Check if a slot is already booked for a doctor
     */
    boolean existsByDoctorIdAndSlotTimestampAndStatus(Long doctorId, LocalDateTime slotTime, String status);

    /**
     * Find all appointments with a specific status
     */
    List<Appointment> findByStatus(String status);
}
