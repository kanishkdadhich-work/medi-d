package com.medid.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionItemResponseDTO {
    
    private Long id;
    private Long medicineId;
    private String medicineName;
    private Integer quantityRequired;
    private Integer availableStock;
}
