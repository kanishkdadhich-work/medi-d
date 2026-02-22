package com.medid.dto;

import lombok.Data;

@Data
public class UserUpdateRequest {
    private String username;
    private String password;
    private String role;
    private Long doctorId;
    private String specialization;
    private Boolean enabled;
}
