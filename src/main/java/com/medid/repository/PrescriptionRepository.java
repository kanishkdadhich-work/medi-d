package com.medid.repository;

import com.medid.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    
    /**
     * Find prescription by appointment ID (One-to-One relationship)
     */
    Optional<Prescription> findByAppointmentId(Long appointmentId);

    /**
     * Find all prescriptions with a specific status
     */
    List<Prescription> findByStatus(String status);

    /**
     * Find all prescriptions for a specific patient (ordered by creation date descending)
     */
    List<Prescription> findByAppointmentPatientIdOrderByCreatedAtDesc(Long patientId);

    /**
     * Count pending prescriptions for a patient
     */
    Long countByAppointmentPatientIdAndStatus(Long patientId, String status);
}
