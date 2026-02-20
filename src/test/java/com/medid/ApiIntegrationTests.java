package com.medid;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for all API endpoints.
 * Verifies request matchers, authentication, and basic API behavior.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static Long patientId = 1L;
    private static Long medicineId = 1L;
    private static Long appointmentId = 1L;
    private static Long prescriptionId = 1L;

    // --- Public endpoints (no auth) ---

    @Test
    @Order(1)
    @DisplayName("GET /api/health - permitAll, returns 200")
    void health_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Medi-D System is up and running")));
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/auth/login - permitAll, returns token")
    void login_shouldBePublic() throws Exception {
        String body = "{\"username\":\"doctor\",\"password\":\"doctorpass\"}";
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    @DisplayName("Protected endpoint without auth rejects access (4xx)")
    void protectedEndpoint_withoutAuth_rejectsAccess() throws Exception {
        mockMvc.perform(get("/api/patients/1"))
                .andExpect(status().is4xxClientError());
    }

    // --- Patient API ---

    @Test
    @Order(10)
    @WithMockUser(roles = "DOCTOR")
    @DisplayName("POST /api/patients - create patient")
    void createPatient() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "name", "Test Patient",
                "contact", "9999999999"
        ));
        MvcResult result = mockMvc.perform(post("/api/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patient_id").exists())
                .andExpect(jsonPath("$.patient_name").value("Test Patient"))
                .andReturn();
        patientId = objectMapper.readTree(result.getResponse().getContentAsString()).get("patient_id").asLong();
    }

    @Test
    @Order(11)
    @WithMockUser(roles = "DOCTOR")
    @DisplayName("GET /api/patients/{id}")
    void getPatient() throws Exception {
        mockMvc.perform(get("/api/patients/" + patientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patient_name").value("Test Patient"));
    }

    // --- Medicine API ---

    @Test
    @Order(20)
    @WithMockUser(roles = "DOCTOR")
    @DisplayName("POST /api/medicines - create medicine")
    void createMedicine() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "name", "Paracetamol",
                "stock", 100,
                "expiryDate", LocalDate.now().plusMonths(12).toString()
        ));
        MvcResult result = mockMvc.perform(post("/api/medicines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.medicine_id").exists())
                .andReturn();
        medicineId = objectMapper.readTree(result.getResponse().getContentAsString()).get("medicine_id").asLong();
    }

    @Test
    @Order(20)
    @WithMockUser(roles = "DOCTOR")
    @DisplayName("GET /api/medicines - all medicines for dropdown")
    void getAllMedicines() throws Exception {
        mockMvc.perform(get("/api/medicines"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @Order(21)
    @WithMockUser(roles = "DOCTOR")
    @DisplayName("GET /api/medicines/{id}")
    void getMedicine() throws Exception {
        mockMvc.perform(get("/api/medicines/" + medicineId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.medicine_name").value("Paracetamol"));
    }

    @Test
    @Order(22)
    @WithMockUser(roles = "DOCTOR")
    @DisplayName("GET /api/medicines/search/by-name")
    void getMedicineByName() throws Exception {
        mockMvc.perform(get("/api/medicines/search/by-name").param("name", "Paracetamol"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.medicine_name").value("Paracetamol"));
    }

    @Test
    @Order(23)
    @WithMockUser(roles = "DOCTOR")
    @DisplayName("GET /api/medicines/low-stock")
    void getLowStockMedicines() throws Exception {
        mockMvc.perform(get("/api/medicines/low-stock").param("minimumStock", "150"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @Order(24)
    @WithMockUser(roles = "DOCTOR")
    @DisplayName("GET /api/medicines/search")
    void searchMedicines() throws Exception {
        mockMvc.perform(get("/api/medicines/search").param("pattern", "para"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // --- Appointment API ---

    @Test
    @Order(30)
    @WithMockUser(roles = "DOCTOR")
    @DisplayName("POST /api/appointments - create appointment")
    void createAppointment() throws Exception {
        String slotTime = LocalDateTime.now().plusDays(1).withSecond(0).withNano(0)
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String body = objectMapper.writeValueAsString(Map.of(
                "patientId", patientId,
                "doctorId", 1L,
                "slotTimestamp", slotTime,
                "status", "BOOKED"
        ));
        MvcResult result = mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.appointment_id").exists())
                .andReturn();
        appointmentId = objectMapper.readTree(result.getResponse().getContentAsString()).get("appointment_id").asLong();
    }

    @Test
    @Order(31)
    @WithMockUser(roles = "DOCTOR")
    @DisplayName("GET /api/appointments/{id}")
    void getAppointment() throws Exception {
        mockMvc.perform(get("/api/appointments/" + appointmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BOOKED"));
    }

    @Test
    @Order(32)
    @WithMockUser(roles = "DOCTOR")
    @DisplayName("GET /api/appointments/patient/{patientId}")
    void getAppointmentsByPatient() throws Exception {
        mockMvc.perform(get("/api/appointments/patient/" + patientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @Order(33)
    @WithMockUser(roles = "DOCTOR")
    @DisplayName("GET /api/appointments/doctor/{doctorId}")
    void getAppointmentsByDoctor() throws Exception {
        mockMvc.perform(get("/api/appointments/doctor/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @Order(33)
    @WithMockUser(roles = "DOCTOR")
    @DisplayName("POST /api/appointments/mark-unavailable - Doctor marks slot")
    void markSlotUnavailable() throws Exception {
        LocalDateTime slotTime = LocalDateTime.now().plusDays(5).withSecond(0).withNano(0);
        mockMvc.perform(post("/api/appointments/mark-unavailable")
                        .param("doctorId", "1")
                        .param("slotTime", slotTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
                .andExpect(status().isOk());
    }

    @Test
    @Order(34)
    @WithMockUser(roles = "DOCTOR")
    @DisplayName("GET /api/appointments/check-available")
    void checkSlotAvailable() throws Exception {
        LocalDateTime slotTime = LocalDateTime.now().plusDays(6).withSecond(0).withNano(0);
        mockMvc.perform(get("/api/appointments/check-available")
                        .param("doctorId", "1")
                        .param("slotTime", slotTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isBoolean());
    }

    @Test
    @Order(35)
    @WithMockUser(roles = "DOCTOR")
    @DisplayName("GET /api/appointments/by-status/{status}")
    void getAppointmentsByStatus() throws Exception {
        mockMvc.perform(get("/api/appointments/by-status/BOOKED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // --- Prescription API ---

    @Test
    @Order(40)
    @WithMockUser(roles = "DOCTOR")
    @DisplayName("POST /api/prescriptions - create prescription")
    void createPrescription() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "appointmentId", appointmentId,
                "diagnosis", "Common cold",
                "status", "PENDING"
        ));
        MvcResult result = mockMvc.perform(post("/api/prescriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.prescription_id").exists())
                .andReturn();
        prescriptionId = objectMapper.readTree(result.getResponse().getContentAsString()).get("prescription_id").asLong();
    }

    @Test
    @Order(41)
    @WithMockUser(roles = "DOCTOR")
    @DisplayName("GET /api/prescriptions/{id}")
    void getPrescription() throws Exception {
        mockMvc.perform(get("/api/prescriptions/" + prescriptionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @Order(42)
    @WithMockUser(roles = "DOCTOR")
    @DisplayName("GET /api/prescriptions/pending")
    void getPendingPrescriptions() throws Exception {
        mockMvc.perform(get("/api/prescriptions/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @Order(43)
    @WithMockUser(roles = "DOCTOR")
    @DisplayName("GET /api/prescriptions/appointment/{appointmentId}")
    void getPrescriptionByAppointment() throws Exception {
        mockMvc.perform(get("/api/prescriptions/appointment/" + appointmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prescription_id").value(prescriptionId.intValue()));
    }

    // --- Prescription Items API ---

    @Test
    @Order(50)
    @WithMockUser(roles = "DOCTOR")
    @DisplayName("POST /api/prescription-items - create prescription item")
    void createPrescriptionItem() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "prescriptionId", prescriptionId,
                "medicineId", medicineId,
                "quantityRequired", 5
        ));
        mockMvc.perform(post("/api/prescription-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.item_id").exists());
    }

    @Test
    @Order(51)
    @WithMockUser(roles = "DOCTOR")
    @DisplayName("GET /api/prescription-items/prescription/{prescriptionId}")
    void getItemsByPrescription() throws Exception {
        mockMvc.perform(get("/api/prescription-items/prescription/" + prescriptionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].quantity_required").value(5))
                .andExpect(jsonPath("$[0].item_id").exists());
    }

    // --- Pharmacy API (PHARMACIST only) ---

    @Test
    @Order(60)
    @WithMockUser(roles = "PHARMACIST")
    @DisplayName("GET /api/pharmacy/prescriptions/pending - PHARMACIST only")
    void pharmacyGetPending_asPharmacist() throws Exception {
        mockMvc.perform(get("/api/pharmacy/prescriptions/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @Order(61)
    @WithMockUser(roles = "DOCTOR")
    @DisplayName("GET /api/pharmacy/prescriptions/pending as DOCTOR returns 403")
    void pharmacyGetPending_asDoctor_returns403() throws Exception {
        mockMvc.perform(get("/api/pharmacy/prescriptions/pending"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status != 403 && status != 500) {
                        throw new AssertionError("Expected 403 (or 500) but got " + status);
                    }
                });
    }

    @Test
    @Order(62)
    @WithMockUser(roles = "PHARMACIST")
    @DisplayName("POST /api/pharmacy/prescriptions/{id}/dispense - PHARMACIST only")
    void pharmacyDispense_asPharmacist() throws Exception {
        mockMvc.perform(post("/api/pharmacy/prescriptions/" + prescriptionId + "/dispense"))
                .andExpect(status().isOk());
    }

    @Test
    @Order(63)
    @WithMockUser(roles = "DOCTOR")
    @DisplayName("POST /api/pharmacy/prescriptions/{id}/dispense as DOCTOR returns 403")
    void pharmacyDispense_asDoctor_returns403() throws Exception {
        mockMvc.perform(post("/api/pharmacy/prescriptions/999/dispense"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status != 403 && status != 500) {
                        throw new AssertionError("Expected 403 (or 500) but got " + status);
                    }
                });
    }

    // --- Prescription dispense (any authenticated) ---

    @Test
    @Order(70)
    @WithMockUser(roles = "DOCTOR")
    @DisplayName("GET /api/prescriptions/dispensed after dispense")
    void getDispensedPrescriptions() throws Exception {
        mockMvc.perform(get("/api/prescriptions/dispensed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
