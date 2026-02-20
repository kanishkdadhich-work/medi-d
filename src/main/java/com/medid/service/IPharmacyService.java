package com.medid.service;

import com.medid.dto.PharmacistPrescriptionDTO;

import java.util.List;

public interface IPharmacyService {

    List<PharmacistPrescriptionDTO> getPendingPrescriptionsForPharmacist();

    void dispensePrescription(Long prescriptionId);
}
