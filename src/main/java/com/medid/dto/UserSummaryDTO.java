package com.medid.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserSummaryDTO {
    private Long id;
    private String username;
    private String role;
    private Long doctorId;
    private String specialization;
    private Boolean enabled;
}
