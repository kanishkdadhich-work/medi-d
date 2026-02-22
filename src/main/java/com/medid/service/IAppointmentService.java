package com.medid.service;

import com.medid.entity.Appointment;

import java.time.LocalDateTime;
import java.util.List;

public interface IAppointmentService {
    Appointment bookAppointment(Long patientId, Long doctorId, LocalDateTime slot);
    Appointment bookAppointment(Long patientId, String doctorRef, LocalDateTime slot);
    void cancelAppointment(Long appointmentId);

    List<Appointment> getAppointmentsByDoctor(Long doctorId);
    void updateStatus(Long id, String status);
    Long resolveDoctorReference(String doctorRef);
    Appointment markUnavailable(Long doctorId, LocalDateTime slot);
    void clearUnavailable(Long doctorId, LocalDateTime slot);


}
