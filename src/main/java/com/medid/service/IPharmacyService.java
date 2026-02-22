package com.medid.service;

import com.medid.entity.Prescription;
import java.util.List;

public interface IPharmacyService {
    void dispensePrescription(Long prescriptionId);
    List<Prescription> getPendingQueue();
}