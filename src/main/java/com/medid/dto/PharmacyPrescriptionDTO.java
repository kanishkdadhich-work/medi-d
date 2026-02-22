package com.medid.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder // <--- This fixes the .builder() error
@AllArgsConstructor
public class PharmacyPrescriptionDTO {
    private Long prescriptionId;
    private String patientName;
    private String doctorName;
    private List<Item> medicines;

    @Data
    @AllArgsConstructor
    public static class Item {
        private String medicineName;
        private Integer quantity;
    }
}