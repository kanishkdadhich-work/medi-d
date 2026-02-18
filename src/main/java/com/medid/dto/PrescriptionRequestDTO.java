package com.medid.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionRequestDTO {
    
    @NotNull(message = "Appointment ID is required")
    @Positive(message = "Appointment ID must be a positive number")
    private Long appointmentId;
    
    private String diagnosis;
    
    @NotBlank(message = "Status is required")
    private String status; // e.g., "PENDING", "DISPENSED"
    
    @NotNull(message = "Prescription items are required")
    private List<PrescriptionItemRequestDTO> items;
}
