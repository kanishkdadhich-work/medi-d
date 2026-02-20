package com.medid.controller;

import com.medid.dto.MedicineRequestDTO;
import com.medid.dto.MedicineResponseDTO;
import com.medid.service.IMedicineService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/medicines")
public class MedicineController {

    @Autowired
    private IMedicineService medicineService;

    @GetMapping
    public ResponseEntity<List<MedicineResponseDTO>> getAllMedicines() {
        return ResponseEntity.ok(medicineService.getAllMedicines());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MedicineResponseDTO> getMedicineById(@PathVariable Long id) {
        log.info("Fetching medicine with ID: {}", id);
        MedicineResponseDTO medicine = medicineService.getMedicineById(id);
        return ResponseEntity.ok(medicine);
    }

    @GetMapping("/search/by-name")
    public ResponseEntity<MedicineResponseDTO> getMedicineByName(@RequestParam String name) {
        log.info("Searching medicine with name: {}", name);
        MedicineResponseDTO medicine = medicineService.getMedicineByName(name);
        return ResponseEntity.ok(medicine);
    }

    @PostMapping
    public ResponseEntity<MedicineResponseDTO> createMedicine(@Valid @RequestBody MedicineRequestDTO medicineRequestDTO) {
        log.info("Creating new medicine: {}", medicineRequestDTO.getName());
        MedicineResponseDTO createdMedicine = medicineService.createMedicine(medicineRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdMedicine);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MedicineResponseDTO> updateMedicine(@PathVariable Long id, @Valid @RequestBody MedicineRequestDTO medicineRequestDTO) {
        log.info("Updating medicine with ID: {}", id);
        MedicineResponseDTO updatedMedicine = medicineService.updateMedicine(id, medicineRequestDTO);
        return ResponseEntity.ok(updatedMedicine);
    }

    @GetMapping("/expiring-before")
    public ResponseEntity<List<MedicineResponseDTO>> getExpiringMedicines(@RequestParam LocalDate expiryDate) {
        log.info("Fetching medicines expiring before: {}", expiryDate);
        List<MedicineResponseDTO> medicines = medicineService.getExpiringMedicines(expiryDate);
        return ResponseEntity.ok(medicines);
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<MedicineResponseDTO>> getLowStockMedicines(@RequestParam Integer minimumStock) {
        log.info("Fetching medicines with stock <= {}", minimumStock);
        List<MedicineResponseDTO> medicines = medicineService.getLowStockMedicines(minimumStock);
        return ResponseEntity.ok(medicines);
    }

    @GetMapping("/search")
    public ResponseEntity<List<MedicineResponseDTO>> searchMedicines(@RequestParam String pattern) {
        log.info("Searching medicines with pattern: {}", pattern);
        List<MedicineResponseDTO> medicines = medicineService.searchMedicines(pattern);
        return ResponseEntity.ok(medicines);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMedicine(@PathVariable Long id) {
        log.info("Deleting medicine with ID: {}", id);
        medicineService.deleteMedicine(id);
        return ResponseEntity.noContent().build();
    }
}
