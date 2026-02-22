package com.medid.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor // Added for flexibility
public class ErrorResponse {
    private LocalDateTime timestamp;
    private String message;
    private String details;
    private int status;

}