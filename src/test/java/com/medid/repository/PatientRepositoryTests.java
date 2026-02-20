package com.medid.repository;

import com.medid.entity.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Patient Repository Tests")
class PatientRepositoryTests {

    @Autowired
    private PatientRepository patientRepository;

    private Patient testPatient;

    @BeforeEach
    void setUp() {
        testPatient = new Patient();
        testPatient.setName("Test Patient");
        testPatient.setContact("9876543210");
        patientRepository.save(testPatient);
    }

    @Test
    @DisplayName("Should find patient by ID")
    void testFindById() {
        Optional<Patient> found = patientRepository.findById(testPatient.getId());
        
        assertTrue(found.isPresent());
        assertEquals("Test Patient", found.get().getName());
        assertEquals("9876543210", found.get().getContact());
    }

    @Test
    @DisplayName("Should save patient successfully")
    void testSavePatient() {
        Patient newPatient = new Patient();
        newPatient.setName("New Patient");
        newPatient.setContact("9999999999");
        
        Patient saved = patientRepository.save(newPatient);
        
        assertNotNull(saved.getId());
        assertEquals("New Patient", saved.getName());
        assertEquals("9999999999", saved.getContact());
    }

    @Test
    @DisplayName("Should return empty for non-existent patient")
    void testFindByIdNotFound() {
        Optional<Patient> found = patientRepository.findById(999999L);
        
        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("Should update patient")
    void testUpdatePatient() {
        testPatient.setName("Updated Name");
        patientRepository.save(testPatient);
        
        Patient updated = patientRepository.findById(testPatient.getId()).orElse(null);
        assertNotNull(updated);
        assertEquals("Updated Name", updated.getName());
    }

    @Test
    @DisplayName("Should delete patient")
    void testDeletePatient() {
        Long patientId = testPatient.getId();
        patientRepository.delete(testPatient);
        
        Optional<Patient> found = patientRepository.findById(patientId);
        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("Should count all patients")
    void testCountPatients() {
        long count = patientRepository.count();
        
        assertTrue(count > 0);
    }
}
