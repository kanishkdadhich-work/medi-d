package com.medid.controller;

import com.medid.dto.AdminOverviewDTO;
import com.medid.dto.UserCreationRequest;
import com.medid.dto.UserSummaryDTO;
import com.medid.dto.UserUpdateRequest;
import com.medid.entity.User;
import com.medid.enums.Role;
import com.medid.enums.PrescriptionStatus;
import com.medid.repository.AppointmentRepository;
import com.medid.repository.MedicineRepository;
import com.medid.repository.PatientRepository;
import com.medid.repository.PrescriptionRepository;
import com.medid.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')") // Hard constraint: Only Admins enter here
public class AdminController {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepo;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final MedicineRepository medicineRepository;

    public AdminController(PasswordEncoder passwordEncoder,
                           UserRepository userRepo,
                           PatientRepository patientRepository,
                           AppointmentRepository appointmentRepository,
                           PrescriptionRepository prescriptionRepository,
                           MedicineRepository medicineRepository) {
        this.passwordEncoder = passwordEncoder;
        this.userRepo = userRepo;
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.medicineRepository = medicineRepository;
    }

    @PostMapping("/users/create")
    public ResponseEntity<String> createUser(@RequestBody UserCreationRequest request) {
        if (userRepo.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Error: Username is already taken!");
        }

        User newUser = new User();
        newUser.setUsername(request.getUsername());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setDoctorId(request.getDoctorId());
        newUser.setSpecialization(request.getSpecialization());

        try {
            // Accept both "DOCTOR" and "ROLE_DOCTOR" payloads.
            String normalizedRole = request.getRole().toUpperCase().replace("ROLE_", "");
            Role selectedRole = Role.valueOf(normalizedRole);
            newUser.setRole(selectedRole);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Error: Invalid Role provided. Available roles are: ADMIN, DOCTOR, PHARMACIST, RECEPTIONIST, PATIENT");
        }

        userRepo.save(newUser);
        if (log.isDebugEnabled()) {
            log.debug("Admin created user '{}' with role '{}'", newUser.getUsername(), newUser.getRole());
        }
        return ResponseEntity.ok("User " + request.getUsername() + " created successfully.");
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserSummaryDTO>> getUsers() {
        List<UserSummaryDTO> users = userRepo.findAll(Sort.by(Sort.Direction.ASC, "username"))
                .stream()
                .map(u -> new UserSummaryDTO(
                        u.getId(),
                        u.getUsername(),
                        u.getRole() == null ? "UNKNOWN" : u.getRole().name(),
                        u.getDoctorId(),
                        u.getSpecialization(),
                        u.getEnabled() == null || u.getEnabled()
                ))
                .toList();
        return ResponseEntity.ok(users);
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<String> updateUser(@PathVariable Long id, @RequestBody UserUpdateRequest request) {
        User existing = userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            existing.setUsername(request.getUsername());
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            existing.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getRole() != null && !request.getRole().isBlank()) {
            String normalizedRole = request.getRole().toUpperCase().replace("ROLE_", "");
            existing.setRole(Role.valueOf(normalizedRole));
        }
        if (request.getDoctorId() != null) {
            existing.setDoctorId(request.getDoctorId());
        }
        if (request.getSpecialization() != null) {
            existing.setSpecialization(request.getSpecialization());
        }
        if (request.getEnabled() != null) {
            existing.setEnabled(request.getEnabled());
        }

        userRepo.save(existing);
        return ResponseEntity.ok("User updated successfully.");
    }

    @PatchMapping("/users/{id}/status")
    public ResponseEntity<String> updateUserStatus(@PathVariable Long id, @RequestParam boolean enabled) {
        User existing = userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        existing.setEnabled(enabled);
        userRepo.save(existing);
        return ResponseEntity.ok("User status updated successfully.");
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable Long id, Authentication authentication) {
        User existing = userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        if (authentication != null && existing.getUsername().equals(authentication.getName())) {
            throw new RuntimeException("You cannot delete your own account.");
        }

        userRepo.delete(existing);
        return ResponseEntity.ok("User deleted successfully.");
    }

    @GetMapping("/overview")
    public ResponseEntity<AdminOverviewDTO> getOverview() {
        AdminOverviewDTO overview = new AdminOverviewDTO(
                userRepo.count(),
                patientRepository.count(),
                appointmentRepository.count(),
                prescriptionRepository.findByStatus(PrescriptionStatus.PENDING).size(),
                prescriptionRepository.findByStatus(PrescriptionStatus.DISPENSED).size(),
                medicineRepository.findLowStockMedicines().size()
        );
        return ResponseEntity.ok(overview);
    }
}
