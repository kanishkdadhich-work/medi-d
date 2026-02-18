package com.medid.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MedicineRequestDTO {
    
    @NotBlank(message = "Medicine name is required")
    private String name;
    
    @NotNull(message = "Stock is required")
    @Positive(message = "Stock must be a positive number")
    private Integer stock;
    
    @NotNull(message = "Expiry date is required")
    private LocalDate expiryDate;
}
