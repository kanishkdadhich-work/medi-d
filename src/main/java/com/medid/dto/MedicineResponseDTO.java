package com.medid.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * DTO for medicine response data.
 * Contains medicine information returned from API endpoints.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MedicineResponseDTO {

    @JsonProperty("medicine_id")
    private Long id;

    @JsonProperty("medicine_name")
    private String name;

    private Integer stock;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @JsonProperty("expiry_date")
    private LocalDate expiryDate;

    @JsonProperty("version")
    private Long version;
}

