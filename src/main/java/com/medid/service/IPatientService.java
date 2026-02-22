package com.medid.service;

import com.medid.entity.Patient;
import java.util.List;

public interface IPatientService {
    Patient registerPatient(Patient patient);
    Patient getPatientById(Long id);
    List<Patient> getAllPatients();
    Patient getByPhoneNumber(String phoneNumber);
    List<Patient> searchPatients(String query);
    Patient updateMedicalBlob(Long patientId, String medicalHistoryBlob);
}
