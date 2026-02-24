package com.medid.repository;

import com.medid.entity.PrescriptionItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrescriptionItemRepository extends JpaRepository<PrescriptionItem, Long> {
    boolean existsByMedicine_MedicineId(Long medicineId);
}

