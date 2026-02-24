package com.medid.service;

import com.medid.entity.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface IPatientService {
    Patient registerPatient(Patient patient);
    Patient getPatientById(Long id);
    List<Patient> getAllPatients();
    Page<Patient> getAllPatients(Pageable pageable);
    Patient getByPhoneNumber(String phoneNumber);
    List<Patient> searchPatients(String query);
    Patient updateMedicalBlob(Long patientId, String medicalHistoryBlob);
}
