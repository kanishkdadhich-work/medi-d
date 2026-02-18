package com.medid.repository;

import com.medid.entity.Medicine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("dev")
@DisplayName("Medicine Repository Tests")
class MedicineRepositoryTests {

    @Autowired
    private MedicineRepository medicineRepository;

    private Medicine testMedicine;

    @BeforeEach
    void setUp() {
        testMedicine = new Medicine();
        testMedicine.setName("Aspirin");
        testMedicine.setStock(100);
        testMedicine.setExpiryDate(LocalDate.now().plusMonths(6));
        medicineRepository.save(testMedicine);
    }

    @Test
    @DisplayName("Should find medicine by name")
    void testFindByName() {
        Optional<Medicine> found = medicineRepository.findByName("Aspirin");
        
        assertTrue(found.isPresent());
        assertEquals("Aspirin", found.get().getName());
        assertEquals(100, found.get().getStock());
    }

    @Test
    @DisplayName("Should find expiring medicines")
    void testFindExpiringMedicines() {
        // Create a medicine expiring soon
        Medicine expiringMedicine = new Medicine();
        expiringMedicine.setName("Expiring Medicine");
        expiringMedicine.setStock(50);
        expiringMedicine.setExpiryDate(LocalDate.now().plusDays(5));
        medicineRepository.save(expiringMedicine);
        
        List<Medicine> expiring = medicineRepository.findByExpiryDateLessThanEqualOrderByExpiryDateAsc(LocalDate.now().plusDays(10));
        
        assertTrue(expiring.size() > 0);
        assertTrue(expiring.stream().anyMatch(m -> m.getName().equals("Expiring Medicine")));
    }

    @Test
    @DisplayName("Should find low stock medicines")
    void testFindLowStockMedicines() {
        // Create a low stock medicine
        Medicine lowStockMedicine = new Medicine();
        lowStockMedicine.setName("Low Stock Medicine");
        lowStockMedicine.setStock(5);
        lowStockMedicine.setExpiryDate(LocalDate.now().plusMonths(6));
        medicineRepository.save(lowStockMedicine);
        
        List<Medicine> lowStock = medicineRepository.findByStockLessThanEqualOrderByStockAsc(10);
        
        assertTrue(lowStock.size() > 0);
        assertTrue(lowStock.stream().anyMatch(m -> m.getName().equals("Low Stock Medicine")));
    }

    @Test
    @DisplayName("Should search medicines by name pattern")
    void testSearchByNamePattern() {
        List<Medicine> results = medicineRepository.findByNameContainingIgnoreCase("aspirin");
        
        assertTrue(results.size() > 0);
        assertTrue(results.stream().anyMatch(m -> m.getName().equalsIgnoreCase("Aspirin")));
    }

    @Test
    @DisplayName("Should search medicines case-insensitively")
    void testSearchCaseInsensitive() {
        List<Medicine> results = medicineRepository.findByNameContainingIgnoreCase("ASPIRIN");
        
        assertTrue(results.size() > 0);
        assertTrue(results.stream().anyMatch(m -> m.getName().equalsIgnoreCase("Aspirin")));
    }

    @Test
    @DisplayName("Should return empty list for non-matching search")
    void testSearchNoResults() {
        List<Medicine> results = medicineRepository.findByNameContainingIgnoreCase("NonExistentMedicine");
        
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Should save medicine successfully")
    void testSaveMedicine() {
        Medicine newMedicine = new Medicine();
        newMedicine.setName("Paracetamol");
        newMedicine.setStock(200);
        newMedicine.setExpiryDate(LocalDate.now().plusMonths(12));
        
        Medicine saved = medicineRepository.save(newMedicine);
        
        assertNotNull(saved.getId());
        assertEquals("Paracetamol", saved.getName());
    }

    @Test
    @DisplayName("Should have optimistic locking version field")
    void testOptimisticLockingVersion() {
        Optional<Medicine> found = medicineRepository.findById(testMedicine.getId());
        
        assertTrue(found.isPresent());
        assertNotNull(found.get().getVersion());
    }
}
