package com.medid.dto;

import lombok.AllArgsConstructor;
import lombok.Data;


    @Data
    @AllArgsConstructor
    public class ConsultationViewDTO {
        private Long appointmentId;
        private String patientName;
        private String phoneNumber;
        private String medicalHistory; // Only included for Doctors
        private String status;
    }

