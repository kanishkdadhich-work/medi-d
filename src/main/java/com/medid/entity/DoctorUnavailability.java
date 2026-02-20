package com.medid.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "doctor_unavailabilities", indexes = {
        @Index(name = "idx_unavail_doctor_slot", columnList = "doctor_id, slot_timestamp")
})
public class DoctorUnavailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "doctor_id")
    private Long doctorId;

    @Column(nullable = false, name = "slot_timestamp")
    private LocalDateTime slotTimestamp;
}
