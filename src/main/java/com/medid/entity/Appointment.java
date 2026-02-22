package com.medid.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointments")
@Data
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long appointmentId;

    @ManyToOne
    @JoinColumn(name = "patient_id")
    private Patient patient;

    private Long doctorId; // In a full system, this would be a @ManyToOne Doctor doctor

    @Column(nullable = false)
    private LocalDateTime appointmentTime;

    // Requirement 3.3: Status Tracking (BOOKED, COMPLETED, CANCELLED)
    private String status = "BOOKED";
}