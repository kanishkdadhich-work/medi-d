package com.medid.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "prescriptions", indexes = {
        @Index(name = "idx_prescription_appointment", columnList = "appointment_id"),
        @Index(name = "idx_prescription_status", columnList = "status")
})
public class Prescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * OneToOne relationship with Appointment
     * Each prescription belongs to exactly one appointment
     * Cascade: ALL to handle prescription lifecycle with appointment
     * Orphan deletion: true to delete prescription if appointment is deleted
     */
    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "appointment_id", nullable = false, unique = true, 
                foreignKey = @ForeignKey(name = "fk_prescription_appointment"))
    private Appointment appointment;

    @Column(name = "diagnosis", columnDefinition = "TEXT")
    private String diagnosis;

    @Column(nullable = false, length = 50)
    private String status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
