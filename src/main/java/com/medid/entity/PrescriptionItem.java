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
@Table(name = "prescription_items", indexes = {
        @Index(name = "idx_prescriptionitem_prescription", columnList = "prescription_id"),
        @Index(name = "idx_prescriptionitem_medicine", columnList = "medicine_id")
})
public class PrescriptionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "prescription_id", nullable = false, 
                foreignKey = @ForeignKey(name = "fk_prescriptionitem_prescription"))
    private Prescription prescription;


    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "medicine_id", nullable = false, 
                foreignKey = @ForeignKey(name = "fk_prescriptionitem_medicine"))
    private Medicine medicine;

    @Column(nullable = false, name = "quantity_required")
    private Integer quantityRequired;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
