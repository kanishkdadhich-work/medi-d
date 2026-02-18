package com.medid.repository;

import com.medid.entity.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MedicineRepository extends JpaRepository<Medicine, Long> {
    
    /**
     * Find medicine by exact name
     */
    Optional<Medicine> findByName(String name);

    /**
     * Find all medicines expiring on or before a specific date (ordered by expiry date ascending)
     */
    List<Medicine> findByExpiryDateLessThanEqualOrderByExpiryDateAsc(LocalDate expiryDate);

    /**
     * Find all medicines with stock less than or equal to given amount (ordered by stock ascending)
     */
    List<Medicine> findByStockLessThanEqualOrderByStockAsc(Integer stock);

    /**
     * Find medicines by name pattern (case-insensitive)
     */
    List<Medicine> findByNameContainingIgnoreCase(String name);
}
