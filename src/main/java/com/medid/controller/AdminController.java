package com.medid.controller;

import com.medid.dto.AdminOverviewDTO;
import com.medid.dto.PagedResponse;
import com.medid.dto.UserCreationRequest;
import com.medid.dto.UserSummaryDTO;
import com.medid.dto.UserUpdateRequest;
import com.medid.entity.User;
import com.medid.enums.Role;
import com.medid.enums.PrescriptionStatus;
import com.medid.exception.ConflictException;
import com.medid.exception.ValidationException;
import com.medid.repository.AppointmentRepository;
import com.medid.repository.MedicineRepository;
import com.medid.repository.PatientRepository;
import com.medid.repository.PrescriptionRepository;
import com.medid.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
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
import java.util.Locale;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')") // Hard constraint: Only Admins enter here
public class AdminController {
    private static final Set<String> SHIFT_VALUES = Set.of("MORNING", "EVENING", "NIGHT");

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepo;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final MedicineRepository medicineRepository;
    private final JdbcTemplate jdbcTemplate;

    public AdminController(PasswordEncoder passwordEncoder,
                           UserRepository userRepo,
                           PatientRepository patientRepository,
                           AppointmentRepository appointmentRepository,
                           PrescriptionRepository prescriptionRepository,
                           MedicineRepository medicineRepository,
                           JdbcTemplate jdbcTemplate) {
        this.passwordEncoder = passwordEncoder;
        this.userRepo = userRepo;
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.medicineRepository = medicineRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("/users/create")
    public ResponseEntity<String> createUser(@RequestBody UserCreationRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new ValidationException("Username is required.");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new ValidationException("Password is required.");
        }
        if (request.getRole() == null || request.getRole().isBlank()) {
            throw new ValidationException("Role is required.");
        }

        if (userRepo.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username is already taken.");
        }

        User newUser = new User();
        newUser.setUsername(request.getUsername());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setDoctorId(request.getDoctorId());
        newUser.setDoctorRefCode(normalize(request.getDoctorRefCode()));
        newUser.setSpecialization(request.getSpecialization());
        newUser.setWeekdayShift(normalizeShift(request.getWeekdayShift()));
        newUser.setWeekendShift(normalizeShift(request.getWeekendShift()));

        try {
            // Accept both "DOCTOR" and "ROLE_DOCTOR" payloads.
            String normalizedRole = request.getRole().toUpperCase().replace("ROLE_", "");
            Role selectedRole = Role.valueOf(normalizedRole);
            newUser.setRole(selectedRole);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid role. Allowed: ADMIN, DOCTOR, PHARMACIST, RECEPTIONIST, PATIENT.");
        }

        validateDoctorFields(newUser, null);
        ensureDoctorProfileLinked(newUser);

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
                        u.getDoctorRefCode(),
                        u.getSpecialization(),
                        u.getWeekdayShift(),
                        u.getWeekendShift(),
                        u.getEnabled() == null || u.getEnabled()
                ))
                .toList();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/users/paged")
    public ResponseEntity<PagedResponse<UserSummaryDTO>> getUsersPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "username") String sortBy,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        Sort sort = "desc".equalsIgnoreCase(direction)
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<UserSummaryDTO> mapped = userRepo.findAll(pageable).map(u -> new UserSummaryDTO(
                u.getId(),
                u.getUsername(),
                u.getRole() == null ? "UNKNOWN" : u.getRole().name(),
                u.getDoctorId(),
                u.getDoctorRefCode(),
                u.getSpecialization(),
                u.getWeekdayShift(),
                u.getWeekendShift(),
                u.getEnabled() == null || u.getEnabled()
        ));

        return ResponseEntity.ok(new PagedResponse<>(
                mapped.getContent(),
                mapped.getNumber(),
                mapped.getSize(),
                mapped.getTotalElements(),
                mapped.getTotalPages(),
                mapped.isFirst(),
                mapped.isLast()
        ));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<String> updateUser(@PathVariable Long id, @RequestBody UserUpdateRequest request) {
        User existing = userRepo.findById(id)
                .orElseThrow(() -> new ValidationException("User not found with id: " + id));

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
        if (request.getDoctorRefCode() != null) {
            existing.setDoctorRefCode(normalize(request.getDoctorRefCode()));
        }
        if (request.getSpecialization() != null) {
            existing.setSpecialization(request.getSpecialization());
        }
        if (request.getWeekdayShift() != null) {
            existing.setWeekdayShift(normalizeShift(request.getWeekdayShift()));
        }
        if (request.getWeekendShift() != null) {
            existing.setWeekendShift(normalizeShift(request.getWeekendShift()));
        }
        if (request.getEnabled() != null) {
            existing.setEnabled(request.getEnabled());
        }

        validateDoctorFields(existing, id);
        ensureDoctorProfileLinked(existing);
        userRepo.save(existing);
        return ResponseEntity.ok("User updated successfully.");
    }

    @PatchMapping("/users/{id}/status")
    public ResponseEntity<String> updateUserStatus(@PathVariable Long id, @RequestParam boolean enabled) {
        User existing = userRepo.findById(id)
                .orElseThrow(() -> new ValidationException("User not found with id: " + id));
        existing.setEnabled(enabled);
        userRepo.save(existing);
        return ResponseEntity.ok("User status updated successfully.");
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable Long id, Authentication authentication) {
        User existing = userRepo.findById(id)
                .orElseThrow(() -> new ValidationException("User not found with id: " + id));

        if (authentication != null && existing.getUsername().equals(authentication.getName())) {
            throw new ConflictException("You cannot delete your own account.");
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

    private void validateDoctorFields(User user, Long selfId) {
        if (user.getRole() != Role.DOCTOR) {
            return;
        }
        if (user.getSpecialization() == null || user.getSpecialization().isBlank()) {
            throw new ValidationException("Specialization is required for DOCTOR.");
        }
        if (user.getDoctorRefCode() == null || user.getDoctorRefCode().isBlank()) {
            throw new ValidationException("Doctor reference ID is required for DOCTOR.");
        }
        if (!user.getDoctorRefCode().matches("^MEDID-[0-9]{2}$")) {
            throw new ValidationException("Doctor reference ID must match MEDID-XX format.");
        }
        if (user.getDoctorId() != null && !doctorProfileExists(user.getDoctorId())) {
            throw new ValidationException("Provided doctorId does not exist in doctors table.");
        }
        boolean hasWeekday = user.getWeekdayShift() != null && !user.getWeekdayShift().isBlank();
        boolean hasWeekend = user.getWeekendShift() != null && !user.getWeekendShift().isBlank();
        if (hasWeekday == hasWeekend) {
            throw new ValidationException("Doctor must be assigned to exactly one day type: WEEKDAY or WEEKEND.");
        }
        if (hasWeekday && !SHIFT_VALUES.contains(user.getWeekdayShift())) {
            throw new ValidationException("Weekday shift must be MORNING, EVENING, or NIGHT.");
        }
        if (hasWeekend && !SHIFT_VALUES.contains(user.getWeekendShift())) {
            throw new ValidationException("Weekend shift must be MORNING, EVENING, or NIGHT.");
        }

        userRepo.findAll().stream()
                .filter(u -> u.getRole() == Role.DOCTOR)
                .filter(u -> u.getDoctorRefCode() != null)
                .filter(u -> u.getDoctorRefCode().equalsIgnoreCase(user.getDoctorRefCode()))
                .filter(u -> selfId == null || !u.getId().equals(selfId))
                .findFirst()
                .ifPresent(u -> {
                    throw new ConflictException("Doctor reference ID already exists.");
                });
    }

    private void ensureDoctorProfileLinked(User user) {
        if (user.getRole() != Role.DOCTOR) {
            return;
        }
        if (user.getDoctorId() != null) {
            return;
        }
        String doctorName = user.getUsername() == null ? "Doctor" : user.getUsername().trim();
        if (!doctorName.toLowerCase(Locale.ROOT).startsWith("dr.")) {
            doctorName = "Dr. " + doctorName;
        }
        Long generatedId = jdbcTemplate.queryForObject(
                "INSERT INTO doctors (name, specialization, is_available) VALUES (?, ?, true) RETURNING doctor_id",
                Long.class,
                doctorName,
                user.getSpecialization()
        );
        if (generatedId == null) {
            throw new ConflictException("Unable to create doctor profile.");
        }
        user.setDoctorId(generatedId);
    }

    private boolean doctorProfileExists(Long doctorId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM doctors WHERE doctor_id = ?",
                Long.class,
                doctorId
        );
        return count != null && count > 0;
    }

    private String normalize(String value) {
        if (value == null) return null;
        return value.trim();
    }

    private String normalizeShift(String value) {
        if (value == null) return null;
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
