package com.medid.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminOverviewDTO {
    private long totalUsers;
    private long totalPatients;
    private long totalAppointments;
    private long pendingPrescriptions;
    private long dispensedPrescriptions;
    private long lowStockMedicines;
}
