package com.medid.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for prescription item request data.
 * Contains prescription item information for creation and update operations.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionItemRequestDTO {

    @NotNull(message = "Prescription ID is required")
    @Positive(message = "Prescription ID must be a positive number")
    private Long prescriptionId;

    @NotNull(message = "Medicine ID is required")
    @Positive(message = "Medicine ID must be a positive number")
    private Long medicineId;

    @NotNull(message = "Quantity required is required")
    @Positive(message = "Quantity must be a positive number")
    private Integer quantityRequired;
}

