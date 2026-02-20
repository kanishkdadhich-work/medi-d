package com.medid.repository;

import com.medid.entity.DoctorUnavailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface DoctorUnavailabilityRepository extends JpaRepository<DoctorUnavailability, Long> {

    boolean existsByDoctorIdAndSlotTimestamp(Long doctorId, LocalDateTime slotTimestamp);
}
