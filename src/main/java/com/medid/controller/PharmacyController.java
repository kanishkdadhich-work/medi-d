package com.medid.controller;

import com.medid.dto.PrescriptionQueueDTO;
import com.medid.entity.Medicine;
import com.medid.entity.Prescription;
import com.medid.repository.MedicineRepository;
import com.medid.service.IPharmacyService;
import com.medid.service.PrescriptionServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/pharmacy")
public class PharmacyController {

    @Autowired
    private IPharmacyService pharmacyService;

    @Autowired
    private MedicineRepository medicineRepo;

    @Autowired
    private PrescriptionServiceImpl prescriptionService;


    // Requirement 4.4 & Module D: Pharmacist Queue (Privacy enforced)
    @GetMapping("/queue")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public ResponseEntity<List<PrescriptionQueueDTO>> getQueue() {
        List<Prescription> pending = pharmacyService.getPendingQueue();

        // Convert to DTO to hide diagnosis notes
        List<PrescriptionQueueDTO> dtos = pending.stream().map(p -> {
            PrescriptionQueueDTO dto = new PrescriptionQueueDTO();
            dto.setPrescriptionId(p.getPrescriptionId());
            dto.setPatientName(p.getAppointment().getPatient().getFullName());
            dto.setMedicineNames(p.getItems().stream()
                    .map(item -> item.getMedicine().getName() + " (Qty: " + item.getQuantity() + ")")
                    .collect(Collectors.toList()));
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // Module C: Dispense with All-or-Nothing Transaction
    @PostMapping("/dispense/{id}")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public ResponseEntity<String> dispense(@PathVariable Long id) {
        pharmacyService.dispensePrescription(id);
        return ResponseEntity.ok("Meds dispensed and stock updated.");
    }

    @GetMapping("/inventory/alerts")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public ResponseEntity<List<Medicine>> getAlerts() {
        return ResponseEntity.ok(medicineRepo.findLowStockMedicines());
    }

    @GetMapping("/reports/daily-summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getDailySummary() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        return ResponseEntity.ok("Prescriptions Dispensed Today: " + prescriptionService.countToday(start));
    }
}
