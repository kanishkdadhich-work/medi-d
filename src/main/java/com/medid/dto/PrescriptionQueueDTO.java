package com.medid.dto;

import lombok.Data;
import java.util.List;

@Data
public class PrescriptionQueueDTO {
    private Long prescriptionId;
    private String patientName;
    private List<String> medicineNames; // Only medicine names, no diagnosis notes!
}