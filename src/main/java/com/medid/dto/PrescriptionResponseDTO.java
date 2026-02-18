package com.medid.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionResponseDTO {
    
    private Long id;
    private Long appointmentId;
    private String diagnosis;
    private String status;
    
    private List<PrescriptionItemResponseDTO> items;
}
