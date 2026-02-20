package com.medid.repository;

import com.medid.entity.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MedicineRepository extends JpaRepository<Medicine, Long> {

    Optional<Medicine> findByName(String name);
    List<Medicine> findByExpiryDateLessThanEqualOrderByExpiryDateAsc(LocalDate expiryDate);
    List<Medicine> findByStockLessThanEqualOrderByStockAsc(Integer stock);
    List<Medicine> findByNameContainingIgnoreCase(String name);

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Medicine m where m.id = :id")
    Optional<Medicine> findByIdForUpdate(@Param("id") Long id);
}
