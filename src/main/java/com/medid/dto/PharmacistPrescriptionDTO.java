package com.medid.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PharmacistPrescriptionDTO {

    @JsonProperty("prescription_id")
    private Long prescriptionId;

    @JsonProperty("items")
    private List<PharmacistPrescriptionItemDTO> items;
}
