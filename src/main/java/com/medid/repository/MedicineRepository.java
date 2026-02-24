package com.medid.repository;

import com.medid.entity.Medicine;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

@Repository
public interface MedicineRepository extends JpaRepository<Medicine, Long> {

    // Pessimistic Locking: Locks the row so other transactions must wait
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Medicine m WHERE m.medicineId = :id")
    Optional<Medicine> findByIdWithLock(Long id);

    List<Medicine> findByNameContainingIgnoreCase(String name);

    @Query("SELECT m FROM Medicine m WHERE m.stockCount <= m.minThreshold")
    List<Medicine> findLowStockMedicines();

    List<Medicine> findByExpiryDateBefore(LocalDate date);
    boolean existsByNameIgnoreCaseAndExpiryDate(String name, LocalDate expiryDate);
    Optional<Medicine> findByNameIgnoreCaseAndExpiryDate(String name, LocalDate expiryDate);
    List<Medicine> findByNameIgnoreCase(String name);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT m FROM Medicine m
            WHERE LOWER(m.name) = LOWER(:name)
            ORDER BY
              CASE WHEN m.expiryDate IS NULL THEN 1 ELSE 0 END,
              m.expiryDate ASC,
              m.medicineId ASC
            """)
    List<Medicine> findByNameIgnoreCaseOrderByExpiryForDispenseWithLock(@Param("name") String name);

    long deleteByExpiryDateBefore(LocalDate date);
}
