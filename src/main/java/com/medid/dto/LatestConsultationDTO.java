package com.medid.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class LatestConsultationDTO {
    private Long prescriptionId;
    private Long appointmentId;
    private Long patientId;
    private String patientName;
    private String diagnosisNotes;
    private LocalDateTime updatedAt;
    private List<String> medicines;
}
