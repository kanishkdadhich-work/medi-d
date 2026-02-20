package com.medid.repository;

import com.medid.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByPatientIdOrderBySlotTimestampDesc(Long patientId);
    List<Appointment> findByDoctorIdOrderBySlotTimestampDesc(Long doctorId);
    List<Appointment> findBySlotTimestampBetweenOrderBySlotTimestampAsc(LocalDateTime startTime, LocalDateTime endTime);
    List<Appointment> findByDoctorIdAndStatusOrderBySlotTimestampAsc(Long doctorId, String status);
    boolean existsByDoctorIdAndSlotTimestampAndStatus(Long doctorId, LocalDateTime slotTime, String status);
    List<Appointment> findByStatus(String status);
}
