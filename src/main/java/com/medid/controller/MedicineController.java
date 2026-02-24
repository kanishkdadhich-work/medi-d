package com.medid.controller;

import com.medid.dto.PagedResponse;
import com.medid.entity.Medicine;
import com.medid.exception.ConflictException;
import com.medid.exception.ValidationException;
import com.medid.repository.MedicineRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
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

    @GetMapping("/paged")
    @PreAuthorize("hasAnyRole('DOCTOR', 'PHARMACIST', 'ADMIN')")
    public ResponseEntity<PagedResponse<Medicine>> getAllMedicinesPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        Sort sort = "desc".equalsIgnoreCase(direction)
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Medicine> result = medicineRepository.findAll(pageable);
        return ResponseEntity.ok(new PagedResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isFirst(),
                result.isLast()
        ));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public Medicine createMedicine(@RequestBody Medicine medicine) {
        log.debug("Create medicine request name={}, stock={}", medicine.getName(), medicine.getStockCount());
        if (medicine.getName() == null || medicine.getName().isBlank()) {
            throw new ValidationException("Medicine name is required.");
        }
        String normalizedName = medicine.getName().trim();
        if (normalizedName.length() > 120) {
            throw new ValidationException("Medicine name is too long.");
        }
        if (normalizedName.contains("<") || normalizedName.contains(">")) {
            throw new ValidationException("Medicine name contains invalid characters.");
        }
        if (medicine.getStockCount() == null || medicine.getStockCount() < 0) {
            throw new ValidationException("stockCount must be zero or greater.");
        }
        if (medicine.getMinThreshold() == null || medicine.getMinThreshold() < 0) {
            throw new ValidationException("minThreshold must be zero or greater.");
        }

        boolean duplicate = medicineRepository.existsByNameIgnoreCaseAndExpiryDate(
                normalizedName,
                medicine.getExpiryDate()
        );
        if (duplicate) {
            throw new ConflictException("Medicine batch already exists for same name and expiry date. Please update existing stock instead.");
        }

        medicine.setName(normalizedName);
        Medicine saved = medicineRepository.save(medicine);
        log.debug("Medicine created medicineId={}", saved.getMedicineId());
        return saved;
    }

    @PutMapping("/{id}/stock")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public ResponseEntity<Medicine> updateMedicineStock(@PathVariable Long id,
                                                        @RequestParam Integer stockCount) {
        if (stockCount == null || stockCount < 0) {
            throw new ValidationException("stockCount must be zero or greater.");
        }
        Medicine medicine = medicineRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Medicine not found"));
        medicine.setStockCount(stockCount);
        Medicine saved = medicineRepository.save(medicine);
        log.debug("Medicine stock updated medicineId={}, stock={}", id, stockCount);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/expired/{id}")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public ResponseEntity<?> deleteExpiredMedicine(@PathVariable Long id) {
        log.debug("Delete single expired medicine request medicineId={}", id);
        Medicine medicine = medicineRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Medicine not found"));

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
    @Transactional
    public ResponseEntity<?> purgeExpiredMedicines() {
        long deleted = medicineRepository.deleteByExpiryDateBefore(LocalDate.now());
        log.debug("Purged expired medicines deletedCount={}", deleted);
        return ResponseEntity.ok(Map.of("deletedCount", deleted));
    }
}
