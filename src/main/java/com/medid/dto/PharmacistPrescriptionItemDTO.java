package com.medid.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PharmacistPrescriptionItemDTO {

    @JsonProperty("medicine_id")
    private Long medicineId;

    @JsonProperty("medicine_name")
    private String medicineName;

    @JsonProperty("quantity_required")
    private Integer quantityRequired;
}
