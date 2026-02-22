package com.medid.repository;

import com.medid.entity.Prescription;
import com.medid.enums.PrescriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    List<Prescription> findByStatus(PrescriptionStatus status);
    java.util.Optional<Prescription> findTopByAppointment_Patient_PatientIdOrderByCreatedAtDesc(Long patientId);


    @Query("SELECT COUNT(p) FROM Prescription p WHERE p.createdAt >= :startOfDay AND p.status = 'DISPENSED'")
    long countDispensedToday(@Param("startOfDay") LocalDateTime startOfDay);

    long countByCreatedAtAfterAndStatus(LocalDateTime start, PrescriptionStatus status);
}
