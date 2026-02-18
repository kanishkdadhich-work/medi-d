package com.medid.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for patient response data.
 * Contains patient information returned from API endpoints.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PatientResponseDTO {

    @JsonProperty("patient_id")
    private Long id;

    @JsonProperty("patient_name")
    private String name;
}

