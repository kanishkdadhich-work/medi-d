package com.medid.repository;

import com.medid.entity.PrescriptionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrescriptionItemRepository extends JpaRepository<PrescriptionItem, Long> {
    
    /**
     * Find all items for a specific prescription (ordered by id ascending)
     */
    List<PrescriptionItem> findByPrescriptionIdOrderByIdAsc(Long prescriptionId);

    /**
     * Find all prescription items that reference a specific medicine (ordered by creation date descending)
     */
    List<PrescriptionItem> findByMedicineIdOrderByCreatedAtDesc(Long medicineId);

    /**
     * Find all pending prescription items for a specific medicine
     */
    List<PrescriptionItem> findByMedicineIdAndPrescriptionStatus(Long medicineId, String status);
}
