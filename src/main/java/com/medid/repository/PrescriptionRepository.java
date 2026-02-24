package com.medid.repository;

import com.medid.entity.Prescription;
import com.medid.enums.PrescriptionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    List<Prescription> findByStatus(PrescriptionStatus status);
    Page<Prescription> findByStatus(PrescriptionStatus status, Pageable pageable);
    java.util.Optional<Prescription> findTopByAppointment_Patient_PatientIdOrderByCreatedAtDesc(Long patientId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Prescription p WHERE p.prescriptionId = :id")
    java.util.Optional<Prescription> findByIdWithLock(@Param("id") Long id);


    @Query("SELECT COUNT(p) FROM Prescription p WHERE p.createdAt >= :startOfDay AND p.status = 'DISPENSED'")
    long countDispensedToday(@Param("startOfDay") LocalDateTime startOfDay);

    long countByCreatedAtAfterAndStatus(LocalDateTime start, PrescriptionStatus status);
    boolean existsByAppointment_AppointmentId(Long appointmentId);
}
