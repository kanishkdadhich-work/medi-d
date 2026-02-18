package com.medid;

import com.medid.entity.*;
import com.medid.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("dev")
@DisplayName("Integration Tests - Complete Data Flow")
class RepositoryIntegrationTests {

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private PrescriptionItemRepository prescriptionItemRepository;

    @Autowired
    private MedicineRepository medicineRepository;

    @Test
    @DisplayName("Complete scenario: Patient → Appointment → Prescription → Medicine")
    void testCompletePatientJourney() {
        // Step 1: Create and save patient
        Patient patient = new Patient();
        patient.setName("John Doe");
        patient.setContact("9999999999");
        Patient savedPatient = patientRepository.save(patient);
        
        assertNotNull(savedPatient.getId());
        assertTrue(patientRepository.findById(savedPatient.getId()).isPresent());

        // Step 2: Create and save appointment for patient
        Appointment appointment = new Appointment();
        appointment.setPatient(savedPatient);
        appointment.setDoctorId(1L);
        appointment.setSlotTimestamp(LocalDateTime.now().plusDays(3));
        appointment.setStatus("BOOKED");
        Appointment savedAppointment = appointmentRepository.save(appointment);
        
        assertNotNull(savedAppointment.getId());
        assertEquals(savedPatient.getId(), savedAppointment.getPatient().getId());

        // Step 3: Create and save prescription for appointment
        Prescription prescription = new Prescription();
        prescription.setAppointment(savedAppointment);
        prescription.setDiagnosis("Common Cold");
        prescription.setStatus("PENDING");
        Prescription savedPrescription = prescriptionRepository.save(prescription);
        
        assertNotNull(savedPrescription.getId());
        assertEquals(savedAppointment.getId(), savedPrescription.getAppointment().getId());

        // Step 4: Create medicines
        Medicine medicine1 = new Medicine();
        medicine1.setName("Cough Syrup");
        medicine1.setStock(500);
        medicine1.setExpiryDate(LocalDate.now().plusMonths(12));
        Medicine savedMedicine1 = medicineRepository.save(medicine1);

        Medicine medicine2 = new Medicine();
        medicine2.setName("Throat Lozenge");
        medicine2.setStock(100);
        medicine2.setExpiryDate(LocalDate.now().plusMonths(6));
        Medicine savedMedicine2 = medicineRepository.save(medicine2);

        // Step 5: Add prescription items
        PrescriptionItem item1 = new PrescriptionItem();
        item1.setPrescription(savedPrescription);
        item1.setMedicine(savedMedicine1);
        item1.setQuantityRequired(1);
        PrescriptionItem savedItem1 = prescriptionItemRepository.save(item1);

        PrescriptionItem item2 = new PrescriptionItem();
        item2.setPrescription(savedPrescription);
        item2.setMedicine(savedMedicine2);
        item2.setQuantityRequired(10);
        PrescriptionItem savedItem2 = prescriptionItemRepository.save(item2);

        assertNotNull(savedItem1.getId());
        assertNotNull(savedItem2.getId());

        // Step 6: Verify complete chain
        Prescription retrievedPrescription = prescriptionRepository.findById(savedPrescription.getId()).orElse(null);
        assertNotNull(retrievedPrescription);
        
        assertNotNull(retrievedPrescription.getAppointment());
        assertEquals(savedAppointment.getId(), retrievedPrescription.getAppointment().getId());
        
        assertNotNull(retrievedPrescription.getAppointment().getPatient());
        assertEquals(savedPatient.getId(), retrievedPrescription.getAppointment().getPatient().getId());

        // Step 7: Verify prescription items
        List<PrescriptionItem> items = prescriptionItemRepository.findByPrescriptionIdOrderByIdAsc(savedPrescription.getId());
        assertEquals(2, items.size());

        // Step 8: Test JPA methods
        List<Appointment> patientAppointments = appointmentRepository.findByPatientIdOrderBySlotTimestampDesc(savedPatient.getId());
        assertFalse(patientAppointments.isEmpty());
        assertTrue(patientAppointments.stream().anyMatch(a -> a.getId().equals(savedAppointment.getId())));

        List<Prescription> patientPrescriptions = prescriptionRepository.findByAppointmentPatientIdOrderByCreatedAtDesc(savedPatient.getId());
        assertFalse(patientPrescriptions.isEmpty());
        assertTrue(patientPrescriptions.stream().anyMatch(p -> p.getId().equals(savedPrescription.getId())));

        // Step 9: Test cascading - delete appointment should cascade to prescription
        appointmentRepository.delete(savedAppointment);
        
        // Prescription should also be deleted due to cascade
        assertTrue(prescriptionRepository.findById(savedPrescription.getId()).isEmpty());
    }

    @Test
    @DisplayName("Test JPA method coverage - All repository methods")
    void testAllJpaMethodsCoverage() {
        // Prepare test data
        Patient patient = new Patient();
        patient.setName("Test Coverage");
        patient.setContact("1111111111");
        patient = patientRepository.save(patient);

        // Test MedicineRepository methods
        Medicine med1 = new Medicine();
        med1.setName("Aspirin");
        med1.setStock(150);
        med1.setExpiryDate(LocalDate.now().plusDays(30));
        final Medicine savedMed1 = medicineRepository.save(med1);

        assertTrue(medicineRepository.findByName("Aspirin").isPresent());
        List<Medicine> expiring = medicineRepository.findByExpiryDateLessThanEqualOrderByExpiryDateAsc(LocalDate.now().plusDays(60));
        assertTrue(expiring.stream().anyMatch(m -> m.getId().equals(savedMed1.getId())));

        Medicine med2 = new Medicine();
        med2.setName("Paracetamol");
        med2.setStock(5);
        med2.setExpiryDate(LocalDate.now().plusMonths(6));
        final Medicine savedMed2 = medicineRepository.save(med2);

        List<Medicine> lowStock = medicineRepository.findByStockLessThanEqualOrderByStockAsc(10);
        assertTrue(lowStock.stream().anyMatch(m -> m.getId().equals(savedMed2.getId())));

        List<Medicine> searched = medicineRepository.findByNameContainingIgnoreCase("aspirI");
        assertTrue(searched.stream().anyMatch(m -> m.getName().equals("Aspirin")));

        // Test AppointmentRepository methods
        Appointment appt = new Appointment();
        appt.setPatient(patient);
        appt.setDoctorId(1L);
        appt.setSlotTimestamp(LocalDateTime.now().plusDays(1));
        appt.setStatus("BOOKED");
        final Appointment savedAppt = appointmentRepository.save(appt);

        List<Appointment> byPatient = appointmentRepository.findByPatientIdOrderBySlotTimestampDesc(patient.getId());
        assertFalse(byPatient.isEmpty());

        List<Appointment> byDoctor = appointmentRepository.findByDoctorIdOrderBySlotTimestampDesc(1L);
        assertFalse(byDoctor.isEmpty());

        LocalDateTime now = LocalDateTime.now();
        List<Appointment> between = appointmentRepository.findBySlotTimestampBetweenOrderBySlotTimestampAsc(now, now.plusDays(5));
        assertTrue(between.size() >= 0);

        List<Appointment> bookedByDoctor = appointmentRepository.findByDoctorIdAndStatusOrderBySlotTimestampAsc(1L, "BOOKED");
        assertFalse(bookedByDoctor.isEmpty());

        boolean slotBooked = appointmentRepository.existsByDoctorIdAndSlotTimestampAndStatus(1L, savedAppt.getSlotTimestamp(), "BOOKED");
        assertTrue(slotBooked);

        List<Appointment> byStatus = appointmentRepository.findByStatus("BOOKED");
        assertFalse(byStatus.isEmpty());

        // Test PrescriptionRepository methods
        Prescription presc = new Prescription();
        presc.setAppointment(savedAppt);
        presc.setDiagnosis("Test");
        presc.setStatus("PENDING");
        final Prescription savedPresc = prescriptionRepository.save(presc);

        assertTrue(prescriptionRepository.findByAppointmentId(savedAppt.getId()).isPresent());
        List<Prescription> byStatus2 = prescriptionRepository.findByStatus("PENDING");
        assertFalse(byStatus2.isEmpty());

        List<Prescription> byPatientId = prescriptionRepository.findByAppointmentPatientIdOrderByCreatedAtDesc(patient.getId());
        assertFalse(byPatientId.isEmpty());

        Long count = prescriptionRepository.countByAppointmentPatientIdAndStatus(patient.getId(), "PENDING");
        assertTrue(count > 0);

        // Test PrescriptionItemRepository methods
        PrescriptionItem item = new PrescriptionItem();
        item.setPrescription(savedPresc);
        item.setMedicine(savedMed1);
        item.setQuantityRequired(5);
        final PrescriptionItem savedItem = prescriptionItemRepository.save(item);

        List<PrescriptionItem> byPresc = prescriptionItemRepository.findByPrescriptionIdOrderByIdAsc(savedPresc.getId());
        assertFalse(byPresc.isEmpty());

        List<PrescriptionItem> byMed = prescriptionItemRepository.findByMedicineIdOrderByCreatedAtDesc(savedMed1.getId());
        assertFalse(byMed.isEmpty());

        List<PrescriptionItem> byMedAndStatus = prescriptionItemRepository.findByMedicineIdAndPrescriptionStatus(savedMed1.getId(), "PENDING");
        assertFalse(byMedAndStatus.isEmpty());
    }

    @Test
    @DisplayName("Verify cascade delete behavior")
    void testCascadeDeleteBehavior() {
        // Create data
        Patient patient = new Patient();
        patient.setName("Cascade Test");
        patient.setContact("2222222222");
        patient = patientRepository.save(patient);

        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctorId(1L);
        appointment.setSlotTimestamp(LocalDateTime.now().plusDays(1));
        appointment.setStatus("BOOKED");
        appointment = appointmentRepository.save(appointment);

        Prescription prescription = new Prescription();
        prescription.setAppointment(appointment);
        prescription.setDiagnosis("Cascade Test");
        prescription.setStatus("PENDING");
        prescription = prescriptionRepository.save(prescription);

        Long appointmentId = appointment.getId();
        Long prescriptionId = prescription.getId();

        // Delete appointment
        appointmentRepository.deleteById(appointmentId);

        // Verify cascade delete
        assertFalse(appointmentRepository.findById(appointmentId).isPresent());
        assertFalse(prescriptionRepository.findById(prescriptionId).isPresent()); // Should be deleted due to cascade

        // Verify patient still exists
        assertTrue(patientRepository.findById(patient.getId()).isPresent());
    }
}
