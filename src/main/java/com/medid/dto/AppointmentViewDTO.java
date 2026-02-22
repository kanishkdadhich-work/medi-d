package com.medid.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class AppointmentViewDTO {
    private Long appointmentId;
    private Long doctorId;
    private LocalDateTime appointmentTime;
    private String status;
    private PatientViewDTO patient;
}
