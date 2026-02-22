package com.medid.service;

import com.medid.dto.PharmacyPrescriptionDTO;
import com.medid.dto.PrescriptionRequest;
import com.medid.entity.Appointment;
import com.medid.entity.Prescription;
import com.medid.entity.PrescriptionItem;
import com.medid.enums.PrescriptionStatus;
import com.medid.repository.AppointmentRepository;
import com.medid.repository.MedicineRepository;
import com.medid.repository.PrescriptionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PrescriptionServiceImpl implements IPrescriptionService {

    @Autowired
    private PrescriptionRepository prescriptionRepo;
    @Autowired
    private AppointmentRepository appointmentRepo;
    @Autowired
    private MedicineRepository medicineRepo;


    @Override
    public long countToday(LocalDateTime start) {
        // This calls the repository method we defined earlier
        long count = prescriptionRepo.countByCreatedAtAfterAndStatus(
                start,
                PrescriptionStatus.DISPENSED
        );
        log.debug("Daily dispensed prescriptions since {} count={}", start, count);
        return count;
    }


    @Override
    @Transactional
    public Prescription createPrescription(PrescriptionRequest request) {
        log.debug("Create prescription request appointmentId={}, items={}",
                request.getAppointmentId(), request.getItems() == null ? 0 : request.getItems().size());
        // 1. Find the appointment
        Appointment app = appointmentRepo.findById(request.getAppointmentId())
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        // 2. Create the Prescription header
        Prescription prescription = new Prescription();
        prescription.setAppointment(app);
        prescription.setDiagnosisNotes(request.getDiagnosisNotes());
        prescription.setStatus(PrescriptionStatus.PENDING); // Waiting for Pharmacist

        // 3. Convert DTO items to Entity items
        List<PrescriptionItem> entityItems = request.getItems().stream().map(dto -> {
            PrescriptionItem item = new PrescriptionItem();
            item.setPrescription(prescription);
            item.setMedicine(medicineRepo.findById(dto.getMedicineId()).orElseThrow());
            item.setQuantity(dto.getQuantity());
            return item;
        }).collect(Collectors.toList());

        prescription.setItems(entityItems);

        // 4. Mark appointment as COMPLETED automatically
        app.setStatus("COMPLETED");
        appointmentRepo.save(app);

        Prescription saved = prescriptionRepo.save(prescription);
        log.debug("Prescription created prescriptionId={} for appointmentId={}",
                saved.getPrescriptionId(), request.getAppointmentId());
        return saved;
    }

    public List<Prescription> getPendingPrescriptions() {
        List<Prescription> pending = prescriptionRepo.findByStatus(PrescriptionStatus.PENDING);
        log.debug("Fetched pending prescriptions count={}", pending.size());
        return pending;
    }

    @Override
    public Prescription getLatestByPatientId(Long patientId) {
        return prescriptionRepo.findTopByAppointment_Patient_PatientIdOrderByCreatedAtDesc(patientId)
                .orElseThrow(() -> new RuntimeException("No consultation found for patientId: " + patientId));
    }

    public List<PharmacyPrescriptionDTO> getPendingPrescriptionsForPharmacy() {
        return prescriptionRepo.findByStatus(PrescriptionStatus.PENDING).stream()
                .map(p -> PharmacyPrescriptionDTO.builder()
                        .prescriptionId(p.getPrescriptionId())
                        .patientName(p.getAppointment().getPatient().getFullName())
                        // Only mapping necessary fields
                        .medicines(p.getItems().stream()
                                .map(i -> new PharmacyPrescriptionDTO.Item(i.getMedicine().getName(), i.getQuantity()))
                                .collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());
    }
}
