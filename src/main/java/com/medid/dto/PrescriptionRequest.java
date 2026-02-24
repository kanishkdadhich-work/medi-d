package com.medid.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class PrescriptionRequest {
    @NotNull(message = "appointmentId is required")
    private Long appointmentId;

    @NotBlank(message = "diagnosisNotes is required")
    private String diagnosisNotes;

    @NotEmpty(message = "At least one medicine item is required")
    @Valid
    private List<MedicationItem> items;

    @Data
    public static class MedicationItem {
        @NotNull(message = "medicineId is required")
        private Long medicineId;

        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "quantity must be at least 1")
        private Integer quantity;
        private String instructions; // e.g., "Twice a day after meals"
    }
}
