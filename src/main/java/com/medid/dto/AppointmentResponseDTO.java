package com.medid.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO for appointment response data.
 * Contains appointment information returned from API endpoints.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentResponseDTO {

    @JsonProperty("appointment_id")
    private Long id;

    @JsonProperty("doctor_id")
    private Long doctorId;

    @JsonProperty("patient_id")
    private Long patientId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonProperty("slot_timestamp")
    private LocalDateTime slotTimestamp;

    private String status;
}

