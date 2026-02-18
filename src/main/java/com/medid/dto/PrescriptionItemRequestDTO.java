package com.medid.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionItemRequestDTO {
    
    @NotNull(message = "Medicine ID is required")
    @Positive(message = "Medicine ID must be a positive number")
    private Long medicineId;
    
    @NotNull(message = "Quantity required is required")
    @Positive(message = "Quantity must be a positive number")
    private Integer quantityRequired;
}
