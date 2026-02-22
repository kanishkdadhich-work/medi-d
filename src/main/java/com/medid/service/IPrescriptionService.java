package com.medid.service;

import com.medid.dto.PrescriptionRequest;
import com.medid.entity.Prescription;

import java.time.LocalDateTime;
import java.util.List;

public interface IPrescriptionService {
    Prescription createPrescription(PrescriptionRequest request);

    List<Prescription> getPendingPrescriptions();

    long countToday(LocalDateTime start);

    Prescription getLatestByPatientId(Long patientId);
}
