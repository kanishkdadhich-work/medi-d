package com.medid.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "patients")
@Data
public class Patient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long patientId;

    @Column(unique = true, length = 32)
    private String patientRefCode;

    @Column(nullable = false)
    @Size(max = 255, message = "fullName cannot exceed 255 characters")
    private String fullName;

    @Column(nullable = false, unique = true)
    @jakarta.validation.constraints.Pattern(regexp = "^\\d{10}$", message = "Invalid Phone Number. Must be 10 digits.")
    private String phoneNumber;

    private String gender;

    // Requirement 1.2: Fetch complete patient details
    @Column(columnDefinition = "TEXT")
    private String medicalHistoryBlob;

    private LocalDateTime createdAt = LocalDateTime.now();
}
