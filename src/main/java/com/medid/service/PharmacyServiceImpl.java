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

        // Lock prescription row to prevent double-dispense races.
        Prescription prescription = prescriptionRepo.findByIdWithLock(prescriptionId)
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
            String medicineName = item.getMedicine().getName();
            int requestedQty = item.getQuantity();

            // FEFO: consume earliest-expiring batch first for same medicine name.
            List<Medicine> batches = medicineRepo.findByNameIgnoreCaseOrderByExpiryForDispenseWithLock(medicineName);
            if (batches.isEmpty()) {
                throw new ResourceNotFoundException("Medicine not found: " + medicineName);
            }

            int totalAvailable = batches.stream().mapToInt(m -> m.getStockCount() == null ? 0 : m.getStockCount()).sum();
            if (totalAvailable < requestedQty) {
                log.debug("Insufficient FEFO stock medicine={}, available={}, required={}",
                        medicineName, totalAvailable, requestedQty);
                throw new InsufficientStockException("Insufficient stock for: " + medicineName +
                        ". Available: " + totalAvailable +
                        ", Required: " + requestedQty);
            }

            int remaining = requestedQty;
            for (Medicine batch : batches) {
                if (remaining <= 0) break;
                int stock = batch.getStockCount() == null ? 0 : batch.getStockCount();
                if (stock <= 0) continue;
                int consume = Math.min(stock, remaining);
                batch.setStockCount(stock - consume);
                remaining -= consume;
                log.debug("FEFO deducted medicineId={}, consumed={}, newStock={}, expiry={}",
                        batch.getMedicineId(), consume, batch.getStockCount(), batch.getExpiryDate());
                medicineRepo.save(batch);
            }
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
