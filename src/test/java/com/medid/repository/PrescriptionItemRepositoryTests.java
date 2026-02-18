package com.medid.repository;

import com.medid.entity.*;
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
@DisplayName("Prescription Item Repository Tests")
class PrescriptionItemRepositoryTests {

    @Autowired
    private PrescriptionItemRepository prescriptionItemRepository;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private MedicineRepository medicineRepository;

    private Patient testPatient;
    private Appointment testAppointment;
    private Prescription testPrescription;
    private Medicine testMedicine;
    private PrescriptionItem testPrescriptionItem;

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

        // Create test medicine
        testMedicine = new Medicine();
        testMedicine.setName("Test Medicine");
        testMedicine.setStock(100);
        testMedicine.setExpiryDate(LocalDate.now().plusMonths(6));
        testMedicine = medicineRepository.save(testMedicine);

        // Create test prescription item
        testPrescriptionItem = new PrescriptionItem();
        testPrescriptionItem.setPrescription(testPrescription);
        testPrescriptionItem.setMedicine(testMedicine);
        testPrescriptionItem.setQuantityRequired(5);
        testPrescriptionItem = prescriptionItemRepository.save(testPrescriptionItem);
    }

    @Test
    @DisplayName("Should find prescription items by prescription ID")
    void testFindByPrescriptionId() {
        List<PrescriptionItem> items = prescriptionItemRepository.findByPrescriptionIdOrderByIdAsc(testPrescription.getId());
        
        assertFalse(items.isEmpty());
        assertTrue(items.stream().anyMatch(pi -> pi.getPrescription().getId().equals(testPrescription.getId())));
    }

    @Test
    @DisplayName("Should find prescription items by medicine ID")
    void testFindByMedicineId() {
        List<PrescriptionItem> items = prescriptionItemRepository.findByMedicineIdOrderByCreatedAtDesc(testMedicine.getId());
        
        assertFalse(items.isEmpty());
        assertTrue(items.stream().anyMatch(pi -> pi.getMedicine().getId().equals(testMedicine.getId())));
    }

    @Test
    @DisplayName("Should find pending prescription items by medicine ID")
    void testFindByMedicineIdAndPendingStatus() {
        List<PrescriptionItem> items = prescriptionItemRepository.findByMedicineIdAndPrescriptionStatus(testMedicine.getId(), "PENDING");
        
        assertFalse(items.isEmpty());
        assertTrue(items.stream().allMatch(pi -> pi.getPrescription().getStatus().equals("PENDING")));
    }

    @Test
    @DisplayName("Should return empty list for non-pending items")
    void testFindByMedicineIdAndDispensedStatus() {
        List<PrescriptionItem> items = prescriptionItemRepository.findByMedicineIdAndPrescriptionStatus(testMedicine.getId(), "DISPENSED");
        
        assertTrue(items.isEmpty());
    }

    @Test
    @DisplayName("Should save prescription item successfully")
    void testSavePrescriptionItem() {
        Medicine newMedicine = new Medicine();
        newMedicine.setName("New Medicine");
        newMedicine.setStock(50);
        newMedicine.setExpiryDate(LocalDate.now().plusMonths(6));
        newMedicine = medicineRepository.save(newMedicine);

        PrescriptionItem newItem = new PrescriptionItem();
        newItem.setPrescription(testPrescription);
        newItem.setMedicine(newMedicine);
        newItem.setQuantityRequired(10);
        
        PrescriptionItem saved = prescriptionItemRepository.save(newItem);
        
        assertNotNull(saved.getId());
        assertEquals(10, saved.getQuantityRequired());
    }

    @Test
    @DisplayName("Should verify many-to-one relationship with Prescription")
    void testPrescriptionRelationship() {
        PrescriptionItem item = prescriptionItemRepository.findById(testPrescriptionItem.getId()).orElse(null);
        
        assertNotNull(item);
        assertNotNull(item.getPrescription());
        assertEquals(testPrescription.getId(), item.getPrescription().getId());
    }

    @Test
    @DisplayName("Should verify many-to-one relationship with Medicine")
    void testMedicineRelationship() {
        PrescriptionItem item = prescriptionItemRepository.findById(testPrescriptionItem.getId()).orElse(null);
        
        assertNotNull(item);
        assertNotNull(item.getMedicine());
        assertEquals(testMedicine.getId(), item.getMedicine().getId());
    }

    @Test
    @DisplayName("Should verify nested relationship through Prescription to Patient")
    void testNestedPatientRelationship() {
        PrescriptionItem item = prescriptionItemRepository.findById(testPrescriptionItem.getId()).orElse(null);
        
        assertNotNull(item);
        assertNotNull(item.getPrescription());
        assertNotNull(item.getPrescription().getAppointment());
        assertNotNull(item.getPrescription().getAppointment().getPatient());
        assertEquals(testPatient.getId(), item.getPrescription().getAppointment().getPatient().getId());
    }

    @Test
    @DisplayName("Should count multiple items in same prescription")
    void testMultipleItemsInPrescription() {
        Medicine medicine2 = new Medicine();
        medicine2.setName("Test Medicine 2");
        medicine2.setStock(50);
        medicine2.setExpiryDate(LocalDate.now().plusMonths(6));
        medicine2 = medicineRepository.save(medicine2);

        PrescriptionItem item2 = new PrescriptionItem();
        item2.setPrescription(testPrescription);
        item2.setMedicine(medicine2);
        item2.setQuantityRequired(3);
        prescriptionItemRepository.save(item2);

        List<PrescriptionItem> items = prescriptionItemRepository.findByPrescriptionIdOrderByIdAsc(testPrescription.getId());
        
        assertEquals(2, items.size());
    }
}
