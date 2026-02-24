package com.medid.repository;

import com.medid.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
    // Requirement 1.1: Automatic validation lookup
    Optional<Patient> findByPhoneNumber(String phoneNumber);
    Optional<Patient> findByPatientRefCodeIgnoreCase(String patientRefCode);

    List<Patient> findTop10ByFullNameContainingIgnoreCaseOrderByFullNameAsc(String name);
    List<Patient> findTop10ByPatientRefCodeContainingIgnoreCaseOrderByPatientRefCodeAsc(String patientRefCode);
}
