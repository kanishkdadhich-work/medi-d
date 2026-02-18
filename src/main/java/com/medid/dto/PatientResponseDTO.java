package com.medid.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PatientResponseDTO {

    private Long id;
    private String name;

    public PatientResponseDTO() {
    }

    public PatientResponseDTO(Long id, String name) {
        this.id = id;
        this.name = name;
    }

}
