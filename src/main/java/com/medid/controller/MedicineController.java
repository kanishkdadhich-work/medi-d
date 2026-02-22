package com.medid.controller;

import com.medid.entity.Medicine;
import com.medid.repository.MedicineRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/medicines")
@Slf4j
public class MedicineController {

    private final MedicineRepository medicineRepository;

    public MedicineController(MedicineRepository medicineRepository) {
        this.medicineRepository = medicineRepository;
    }

    // This is for the Doctor's "Live Search" dropdown
    @GetMapping("/search")
    public List<Medicine> searchMedicines(@RequestParam String name) {
        List<Medicine> medicines = medicineRepository.findByNameContainingIgnoreCase(name);
        log.debug("Medicine search query={} resultCount={}", name, medicines.size());
        return medicines;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DOCTOR', 'PHARMACIST', 'ADMIN')")
    public List<Medicine> getAllMedicines() {
        List<Medicine> medicines = medicineRepository.findAll();
        log.debug("Fetched all medicines count={}", medicines.size());
        return medicines;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public Medicine createMedicine(@RequestBody Medicine medicine) {
        log.debug("Create medicine request name={}, stock={}", medicine.getName(), medicine.getStockCount());
        Medicine saved = medicineRepository.save(medicine);
        log.debug("Medicine created medicineId={}", saved.getMedicineId());
        return saved;
    }

    @DeleteMapping("/expired/{id}")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public ResponseEntity<?> deleteExpiredMedicine(@PathVariable Long id) {
        log.debug("Delete single expired medicine request medicineId={}", id);
        Medicine medicine = medicineRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Medicine not found"));

        if (medicine.getExpiryDate() == null || !medicine.getExpiryDate().isBefore(LocalDate.now())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Medicine is not expired and cannot be deleted.",
                    "error_code", "NOT_EXPIRED"
            ));
        }

        medicineRepository.delete(medicine);
        log.debug("Deleted expired medicine medicineId={}", id);
        return ResponseEntity.ok(Map.of("deletedMedicineId", id));
    }

    @DeleteMapping("/expired")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public ResponseEntity<?> purgeExpiredMedicines() {
        long deleted = medicineRepository.deleteByExpiryDateBefore(LocalDate.now());
        log.debug("Purged expired medicines deletedCount={}", deleted);
        return ResponseEntity.ok(Map.of("deletedCount", deleted));
    }
}
