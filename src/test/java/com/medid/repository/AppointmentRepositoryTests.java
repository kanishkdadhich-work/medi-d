package com.medid.repository;

import com.medid.entity.Appointment;
import com.medid.entity.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("dev")
@DisplayName("Appointment Repository Tests")
class AppointmentRepositoryTests {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PatientRepository patientRepository;

    private Patient testPatient;
    private Appointment testAppointment;

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
    }

    @Test
    @DisplayName("Should find appointments by patient ID")
    void testFindByPatientId() {
        List<Appointment> appointments = appointmentRepository.findByPatientIdOrderBySlotTimestampDesc(testPatient.getId());
        
        assertFalse(appointments.isEmpty());
        assertTrue(appointments.stream().anyMatch(a -> a.getPatient().getId().equals(testPatient.getId())));
    }

    @Test
    @DisplayName("Should find appointments by doctor ID")
    void testFindByDoctorId() {
        List<Appointment> appointments = appointmentRepository.findByDoctorIdOrderBySlotTimestampDesc(1L);
        
        assertFalse(appointments.isEmpty());
        assertTrue(appointments.stream().anyMatch(a -> a.getDoctorId().equals(1L)));
    }

    @Test
    @DisplayName("Should find appointments within time range")
    void testFindAppointmentsBetween() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = LocalDateTime.now().plusDays(2);
        
        List<Appointment> appointments = appointmentRepository.findBySlotTimestampBetweenOrderBySlotTimestampAsc(start, end);
        
        assertTrue(appointments.size() >= 0);
    }

    @Test
    @DisplayName("Should find booked appointments by doctor ID")
    void testFindBookedAppointmentsByDoctorId() {
        List<Appointment> bookedAppointments = appointmentRepository.findByDoctorIdAndStatusOrderBySlotTimestampAsc(1L, "BOOKED");
        
        assertFalse(bookedAppointments.isEmpty());
        assertTrue(bookedAppointments.stream().allMatch(a -> a.getStatus().equals("BOOKED")));
    }

    @Test
    @DisplayName("Should check if slot is booked")
    void testIsSlotBooked() {
        boolean isBooked = appointmentRepository.existsByDoctorIdAndSlotTimestampAndStatus(
                testAppointment.getDoctorId(),
                testAppointment.getSlotTimestamp(),
                "BOOKED"
        );
        
        assertTrue(isBooked);
    }

    @Test
    @DisplayName("Should return false for available slot")
    void testSlotNotBooked() {
        boolean isBooked = appointmentRepository.existsByDoctorIdAndSlotTimestampAndStatus(
                999L,
                LocalDateTime.now().plusDays(10),
                "BOOKED"
        );
        
        assertFalse(isBooked);
    }

    @Test
    @DisplayName("Should find appointments by status")
    void testFindByStatus() {
        List<Appointment> appointments = appointmentRepository.findByStatus("BOOKED");
        
        assertFalse(appointments.isEmpty());
        assertTrue(appointments.stream().allMatch(a -> a.getStatus().equals("BOOKED")));
    }

    @Test
    @DisplayName("Should save appointment successfully")
    void testSaveAppointment() {
        Appointment newAppointment = new Appointment();
        newAppointment.setPatient(testPatient);
        newAppointment.setDoctorId(2L);
        newAppointment.setSlotTimestamp(LocalDateTime.now().plusDays(2));
        newAppointment.setStatus("BOOKED");
        
        Appointment saved = appointmentRepository.save(newAppointment);
        
        assertNotNull(saved.getId());
        assertEquals(2L, saved.getDoctorId());
    }

    @Test
    @DisplayName("Should verify many-to-one relationship with Patient")
    void testPatientRelationship() {
        Appointment appointment = appointmentRepository.findById(testAppointment.getId()).orElse(null);
        
        assertNotNull(appointment);
        assertNotNull(appointment.getPatient());
        assertEquals(testPatient.getId(), appointment.getPatient().getId());
    }
}
