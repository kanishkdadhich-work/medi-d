package com.medid.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DoctorRefDTO {
    private Long appointmentDoctorId;
    private Long userId;
    private String username;
    private String doctorRefCode;
    private String specialization;
    private String weekdayShift;
    private String weekendShift;
}
