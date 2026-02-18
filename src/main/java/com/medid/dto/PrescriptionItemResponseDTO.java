package com.medid.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for prescription item response data.
 * Contains prescription item information returned from API endpoints.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionItemResponseDTO {

    @JsonProperty("item_id")
    private Long id;

    @JsonProperty("prescription_id")
    private Long prescriptionId;

    @JsonProperty("medicine_id")
    private Long medicineId;

    @JsonProperty("medicine_name")
    private String medicineName;

    @JsonProperty("quantity_required")
    private Integer quantityRequired;
}

