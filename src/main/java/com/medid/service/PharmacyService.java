package com.medid.service;

import com.medid.dto.PharmacistPrescriptionDTO;
import com.medid.dto.PharmacistPrescriptionItemDTO;
import com.medid.entity.Medicine;
import com.medid.entity.Prescription;
import com.medid.entity.PrescriptionItem;
import com.medid.exception.InsufficientStockException;
import com.medid.exception.ResourceNotFoundException;
import com.medid.repository.MedicineRepository;
import com.medid.repository.PrescriptionItemRepository;
import com.medid.repository.PrescriptionRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class PharmacyService implements IPharmacyService {

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private PrescriptionItemRepository prescriptionItemRepository;

    @Autowired
    private MedicineRepository medicineRepository;

    @Override
    public List<PharmacistPrescriptionDTO> getPendingPrescriptionsForPharmacist() {
        List<Prescription> pending = prescriptionRepository.findByStatus("PENDING");
        List<PharmacistPrescriptionDTO> out = new ArrayList<>();

        for (Prescription p : pending) {
            List<PrescriptionItem> items = prescriptionItemRepository.findByPrescriptionIdOrderByIdAsc(p.getId());
            List<PharmacistPrescriptionItemDTO> dtoItems = new ArrayList<>();
            for (PrescriptionItem it : items) {
                dtoItems.add(new PharmacistPrescriptionItemDTO(
                        it.getMedicine().getId(),
                        it.getMedicine().getName(),
                        it.getQuantityRequired()
                ));
            }
            out.add(new PharmacistPrescriptionDTO(p.getId(), dtoItems));
        }

        return out;
    }


    @Override
    @Transactional
    public void dispensePrescription(Long prescriptionId) {
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found: " + prescriptionId));

        if (!"PENDING".equalsIgnoreCase(prescription.getStatus())) {
            throw new IllegalStateException("Prescription is not in PENDING state: " + prescriptionId);
        }

        List<PrescriptionItem> items = prescriptionItemRepository.findByPrescriptionIdOrderByIdAsc(prescriptionId);

        List<Medicine> locked = new ArrayList<>();
        for (PrescriptionItem item : items) {
            Medicine med = medicineRepository.findByIdForUpdate(item.getMedicine().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Medicine not found: " + item.getMedicine().getId()));

            if (med.getStock() < item.getQuantityRequired()) {
                log.warn("Insufficient stock for medicine {}: required={} available={}", med.getId(), item.getQuantityRequired(), med.getStock());
                throw new InsufficientStockException("Insufficient stock for medicine: " + med.getName());
            }

            locked.add(med);
        }

        for (PrescriptionItem item : items) {
            Medicine med = locked.stream().filter(m -> m.getId().equals(item.getMedicine().getId())).findFirst().get();
            med.setStock(med.getStock() - item.getQuantityRequired());
            medicineRepository.save(med);
        }

        prescription.setStatus("DISPENSED");
        prescriptionRepository.save(prescription);
    }
}
