package com.medid.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class PatientViewDTO {
    private Long patientId;
    private String patientRefCode;
    private String fullName;
    private String phoneNumber;
    private String gender;
    private String medicalHistoryBlob;
    private LocalDateTime createdAt;
}
