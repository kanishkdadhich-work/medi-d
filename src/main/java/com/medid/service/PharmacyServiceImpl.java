package com.medid.service;

import com.medid.entity.Medicine;
import com.medid.entity.Prescription;
import com.medid.entity.PrescriptionItem;
import com.medid.enums.PrescriptionStatus;
import com.medid.exception.InsufficientStockException;
import com.medid.exception.ResourceNotFoundException;
import com.medid.repository.MedicineRepository;
import com.medid.repository.PrescriptionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class PharmacyServiceImpl implements IPharmacyService {

    @Autowired
    private MedicineRepository medicineRepo;

    @Autowired
    private PrescriptionRepository prescriptionRepo;

    @Override
    @Transactional // Requirement 5.1: Ensures All-or-Nothing rollback
    public void dispensePrescription(Long prescriptionId) {
        log.debug("Dispense prescription request prescriptionId={}", prescriptionId);
        // 1. Fetch the prescription

        Prescription prescription = prescriptionRepo.findById(prescriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found with ID: " + prescriptionId));

        if (prescription.getStatus() == PrescriptionStatus.DISPENSED) {
            log.debug("Dispense blocked, already dispensed prescriptionId={}", prescriptionId);
            throw new RuntimeException("This prescription has already been dispensed!");
        }

        // 2. Check if it's already processed using the external Enum
        if (prescription.getStatus() != PrescriptionStatus.PENDING) {
            log.debug("Dispense blocked due to invalid status prescriptionId={}, status={}", prescriptionId, prescription.getStatus());
            throw new RuntimeException("Prescription is not in PENDING status. Current status: " + prescription.getStatus());
        }

        // 3. Process each item in the prescription
        for (PrescriptionItem item : prescription.getItems()) {

            // Requirement 2.6: Pessimistic Locking - locks the medicine row in DB
            Medicine med = medicineRepo.findByIdWithLock(item.getMedicine().getMedicineId())
                    .orElseThrow(() -> new ResourceNotFoundException("Medicine not found: " + item.getMedicine().getName()));

            // 4. Validate stock levels
            if (med.getStockCount() < item.getQuantity()) {
                log.debug("Insufficient stock medicineId={}, available={}, required={}",
                        med.getMedicineId(), med.getStockCount(), item.getQuantity());
                // Throwing this triggers the @Transactional rollback and the Global Exception Handler
                throw new InsufficientStockException("Insufficient stock for: " + med.getName() +
                        ". Available: " + med.getStockCount() +
                        ", Required: " + item.getQuantity());
            }

            // 5. Deduct stock
            med.setStockCount(med.getStockCount() - item.getQuantity());
            log.debug("Stock deducted medicineId={}, newStock={}", med.getMedicineId(), med.getStockCount());

            // 6. Save updated medicine (row remains locked until method ends)
            medicineRepo.save(med);
        }

        // 7. Update prescription status
        prescription.setStatus(PrescriptionStatus.DISPENSED);
        prescriptionRepo.save(prescription);
        log.debug("Prescription dispensed prescriptionId={}", prescriptionId);
    }

    @Override
    public List<Prescription> getPendingQueue() {
        // Returns the list of prescriptions filtered by the PENDING enum
        List<Prescription> pending = prescriptionRepo.findByStatus(PrescriptionStatus.PENDING);
        log.debug("Fetched pending pharmacy queue count={}", pending.size());
        return pending;
    }


}
