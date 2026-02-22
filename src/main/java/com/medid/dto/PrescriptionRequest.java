package com.medid.dto;

import lombok.Data;

import java.util.List;

@Data
public class PrescriptionRequest {
    private Long appointmentId;
    private String diagnosisNotes;
    private List<MedicationItem> items;

    @Data
    public static class MedicationItem {
        private Long medicineId;
        private Integer quantity;
        private String instructions; // e.g., "Twice a day after meals"
    }
}
