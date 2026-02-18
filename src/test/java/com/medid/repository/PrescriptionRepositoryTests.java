package com.medid.repository;

import com.medid.entity.Appointment;
import com.medid.entity.Patient;
import com.medid.entity.Prescription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("dev")
@DisplayName("Prescription Repository Tests")
class PrescriptionRepositoryTests {

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PatientRepository patientRepository;

    private Patient testPatient;
    private Appointment testAppointment;
    private Prescription testPrescription;

    @BeforeEach
    void setUp() {
        // Create test patient
        testPatient = new Patient();
        testPatient.setName("Test Patient");
        testPatient.setContact("9876543210");
        testPatient = patientRepository.save(testPatient);

        // Create test appointment
        testAppointment = new Appointment();
        testAppointment.setPatient(testPatient);
        testAppointment.setDoctorId(1L);
        testAppointment.setSlotTimestamp(LocalDateTime.now().plusDays(1));
        testAppointment.setStatus("BOOKED");
        testAppointment = appointmentRepository.save(testAppointment);

        // Create test prescription
        testPrescription = new Prescription();
        testPrescription.setAppointment(testAppointment);
        testPrescription.setDiagnosis("Test Diagnosis");
        testPrescription.setStatus("PENDING");
        testPrescription = prescriptionRepository.save(testPrescription);
    }

    @Test
    @DisplayName("Should find prescription by appointment ID")
    void testFindByAppointmentId() {
        Optional<Prescription> found = prescriptionRepository.findByAppointmentId(testAppointment.getId());
        
        assertTrue(found.isPresent());
        assertEquals(testAppointment.getId(), found.get().getAppointment().getId());
    }

    @Test
    @DisplayName("Should find prescriptions by status")
    void testFindByStatus() {
        List<Prescription> prescriptions = prescriptionRepository.findByStatus("PENDING");
        
        assertFalse(prescriptions.isEmpty());
        assertTrue(prescriptions.stream().allMatch(p -> p.getStatus().equals("PENDING")));
    }

    @Test
    @DisplayName("Should find prescriptions by patient ID")
    void testFindByPatientId() {
        List<Prescription> prescriptions = prescriptionRepository.findByAppointmentPatientIdOrderByCreatedAtDesc(testPatient.getId());
        
        assertFalse(prescriptions.isEmpty());
        assertTrue(prescriptions.stream().anyMatch(p -> p.getAppointment().getPatient().getId().equals(testPatient.getId())));
    }

    @Test
    @DisplayName("Should count pending prescriptions for patient")
    void testCountPendingByPatientId() {
        Long count = prescriptionRepository.countByAppointmentPatientIdAndStatus(testPatient.getId(), "PENDING");
        
        assertTrue(count > 0);
    }

    @Test
    @DisplayName("Should return 0 count for no pending prescriptions")
    void testCountPendingNoResults() {
        Long count = prescriptionRepository.countByAppointmentPatientIdAndStatus(999999L, "PENDING");
        
        assertEquals(0L, count);
    }

    @Test
    @DisplayName("Should save prescription successfully")
    void testSavePrescription() {
        Prescription newPrescription = new Prescription();
        newPrescription.setAppointment(testAppointment);
        newPrescription.setDiagnosis("New Diagnosis");
        newPrescription.setStatus("PENDING");
        
        Prescription saved = prescriptionRepository.save(newPrescription);
        
        assertNotNull(saved.getId());
        assertEquals("New Diagnosis", saved.getDiagnosis());
    }

    @Test
    @DisplayName("Should verify one-to-one relationship with Appointment")
    void testAppointmentRelationship() {
        Prescription prescription = prescriptionRepository.findById(testPrescription.getId()).orElse(null);
        
        assertNotNull(prescription);
        assertNotNull(prescription.getAppointment());
        assertEquals(testAppointment.getId(), prescription.getAppointment().getId());
    }

    @Test
    @DisplayName("Should verify nested relationship with Patient through Appointment")
    void testPatientNestedRelationship() {
        Prescription prescription = prescriptionRepository.findById(testPrescription.getId()).orElse(null);
        
        assertNotNull(prescription);
        assertNotNull(prescription.getAppointment());
        assertNotNull(prescription.getAppointment().getPatient());
        assertEquals(testPatient.getId(), prescription.getAppointment().getPatient().getId());
    }
}
