package com.medid.dto;

import lombok.Data;

@Data // This generates getUsername(), getPassword(), etc.
public class UserCreationRequest {
    private String username;
    private String password;
    private String role; // e.g., "ROLE_DOCTOR"
    private Long doctorId;
    private String doctorRefCode;
    private String specialization;
    private String weekdayShift;
    private String weekendShift;
}
