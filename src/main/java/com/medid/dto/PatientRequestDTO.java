package com.medid.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PatientRequestDTO {

    @NotBlank(message = "Patient name is required and cannot be empty")
    private String name;

    @NotBlank(message = "Patient contact is required and cannot be empty")
    @Pattern(
        regexp = "^[+]?[0-9]{1,3}?[-.]?[(]?[0-9]{1,4}[)]?[-.]?[0-9]{1,4}[-.]?[0-9]{1,9}$",
        message = "Contact must be a valid phone number (e.g., +1-555-0100 or 5550100)"
    )
    private String contact;
}
