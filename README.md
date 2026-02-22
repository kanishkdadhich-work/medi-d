# Medi-D

Medical & Inventory Management System with role-based workflows for:
- `ADMIN`
- `RECEPTIONIST`
- `DOCTOR`
- `PHARMACIST`

Backend is a Spring Boot REST API, frontend is a React + Vite + Tailwind app in `UI/`.

## 1. Tech Stack

### Backend
- Java 21
- Spring Boot 3.5.x
- Spring Security (JWT-based auth)
- Spring Data JPA + Hibernate
- PostgreSQL
- Lombok

### Frontend
- React 18
- Vite 5
- Tailwind CSS

## 2. High-Level Architecture

- Frontend (`UI`) calls backend REST APIs under `/api/**`.
- Authentication:
  - Login returns JWT
  - JWT is sent as HTTP-only cookie (`medid_token`) and can also be used via `Authorization: Bearer <token>`
- Authorization:
  - Role-based endpoint protection in `SecurityConfig`
  - Additional method-level checks via `@PreAuthorize`
- Domain modules:
  - Appointments
  - Patients
  - Prescriptions
  - Pharmacy inventory/dispensing
  - Admin user management

## 3. Database Schema

> Note: Hibernate DDL mode is `update`, so schema is managed from entities.

### 3.1 `users`
Represents staff users (and Spring Security principals).

Columns:
- `id` (PK)
- `username` (unique, not null)
- `password` (BCrypt hash)
- `role` (`ADMIN|DOCTOR|PHARMACIST|RECEPTIONIST|...` enum)
- `doctor_id` (logical doctor identifier used by appointment flow)
- `specialization` (doctor specialization for UX and filtering)
- `enabled` (soft enable/disable)

Reasoning:
- Keep auth + role + profile in one table for simpler RBAC.
- `doctor_id` is stored because appointments currently reference `doctorId` as scalar, not FK.

### 3.2 `patients`
Patient master data.

Columns:
- `patient_id` (PK)
- `full_name`
- `phone_number` (unique)
- `gender`
- `medical_history_blob` (TEXT)
- `created_at`

Reasoning:
- Unique phone enables “find existing patient” flow.
- Medical blob is restricted by role (doctor/admin visibility).

### 3.3 `appointments`
Booking and doctor schedule records.

Columns:
- `appointment_id` (PK)
- `patient_id` (FK -> `patients.patient_id`, nullable for unavailable slots)
- `doctor_id` (scalar doctor reference)
- `appointment_time`
- `status` (`SCHEDULED|BOOKED|COMPLETED|CANCELLED|UNAVAILABLE`)

Reasoning:
- `UNAVAILABLE` is modeled as an appointment row without patient, letting doctor block slots.
- Historical rows are retained; rebooking can reuse `CANCELLED/COMPLETED` slot rows.

### 3.4 `prescriptions`
Consultation output created by doctor.

Columns:
- `prescription_id` (PK)
- `status` (`PENDING|DISPENSED|CANCELLED` enum)
- `appointment_id` (FK -> `appointments.appointment_id`)
- `diagnosis_notes` (TEXT)
- `created_at`

Reasoning:
- Prescription is linked to appointment for audit trail and workflow continuity.

### 3.5 `prescription_items`
Line items for medicines in a prescription.

Columns:
- `item_id` (PK)
- `prescription_id` (FK -> `prescriptions.prescription_id`)
- `medicine_id` (FK -> `medicines.medicine_id`)
- `quantity`

Reasoning:
- Normalized line-item model allows many medicines per prescription.

### 3.6 `medicines`
Medicine inventory.

Columns:
- `medicine_id` (PK)
- `name`
- `stock_count`
- `min_threshold`
- `expiry_date`

Reasoning:
- `min_threshold` drives low-stock alerts.
- `expiry_date` supports expired stock deletion.

## 4. Entity Relationships (and why)

- `Patient (1) -> (N) Appointment`
  - A patient can have many appointments over time.
- `Appointment (1) -> (N) Prescription` (practically one active/latest in workflows)
  - Consultation history can be audited.
- `Prescription (1) -> (N) PrescriptionItem`
  - One prescription can contain multiple medicines.
- `PrescriptionItem (N) -> (1) Medicine`
  - Many prescriptions can reference the same medicine stock item.

Design tradeoff:
- `Appointment.doctorId` is scalar instead of FK relation to `User`; this keeps current model simple but is less strict than a relational FK.

## 5. Core Business Rules

### Appointments
- Receptionist/admin can book appointments.
- Doctor can mark a slot as unavailable.
- Slot booking conflicts with statuses: `SCHEDULED`, `BOOKED`, `UNAVAILABLE`.
- `CANCELLED` and `COMPLETED` slots are rebookable.
- Same patient cannot have multiple active appointments with same doctor on same day.
- Rebooking same doctor/time after cancel/complete reuses historical row to avoid unique-slot conflicts.

### Consultation / Prescription
- Doctor/admin can create prescriptions.
- Creating prescription marks appointment `COMPLETED`.
- Doctor/admin can fetch latest consultation by patient.

### Pharmacy
- Pharmacist/admin sees pending prescriptions.
- Dispense is all-or-nothing transaction.
- Stock is deducted with pessimistic row locking.
- Low stock alerts and expired stock cleanup are supported.

### Privacy
- `medicalHistoryBlob` visible only to doctor/admin.
- Pharmacist views avoid diagnosis details in queue DTO flows.

## 6. Workflows

### 6.1 Receptionist Booking Workflow
1. Search/select existing patient by ID/name/phone or register new one.
2. Select doctor (ID/username; specialization visible).
3. Pick slot.
4. Backend validates conflicts + patient-doctor-day rule.
5. Appointment created with `SCHEDULED`.

### 6.2 Doctor Consultation Workflow
1. Doctor opens queue (`Today`, `Completed`, `Future`, `Calendar`).
2. Can update patient medical blob.
3. Adds diagnosis + medicines (live searchable dropdown).
4. Submits prescription.
5. Appointment becomes `COMPLETED`.
6. Latest consultation is viewable for the patient.

### 6.3 Doctor Calendar Workflow
1. Doctor selects date.
2. Marks slot `UNAVAILABLE` or clears unavailability.
3. Receptionist sees unavailable slots as blocked.

### 6.4 Pharmacist Dispense Workflow
1. View pending prescriptions.
2. Dispense selected prescription.
3. Backend locks medicine rows, validates stock, deducts inventory.
4. Prescription status changes to `DISPENSED`.

### 6.5 Admin Workflow
1. Create/update/enable/disable/delete staff users.
2. Maintain doctor metadata (`doctorId`, specialization).
3. View system overview KPIs.

## 7. API Surface (Summary)

### Auth
- `POST /api/auth/login`
- `POST /api/auth/logout`
- `GET /api/auth/me`
- `POST /api/auth/signup`

### Admin (`ADMIN`)
- `GET /api/admin/overview`
- `GET /api/admin/users`
- `POST /api/admin/users/create`
- `PUT /api/admin/users/{id}`
- `PATCH /api/admin/users/{id}/status`
- `DELETE /api/admin/users/{id}`

### Appointments
- `GET /api/appointments` (`RECEPTIONIST`, `ADMIN`)
- `POST /api/appointments/book` (`RECEPTIONIST`, `ADMIN`)
- `POST /api/appointments/book-flex` (`RECEPTIONIST`, `ADMIN`)
- `POST /api/appointments/{id}/cancel` (`DOCTOR`, `RECEPTIONIST`, `ADMIN`)
- `PATCH /api/appointments/{id}/status` (`DOCTOR`, `RECEPTIONIST`, `ADMIN`)
- `POST /api/appointments/{id}/complete` (`DOCTOR`, `ADMIN`)
- `GET /api/appointments/doctors` (`RECEPTIONIST`, `ADMIN`)
- `GET /api/appointments/doctor/my` (`DOCTOR`)
- `GET /api/appointments/doctor/today` (`DOCTOR`)
- `POST /api/appointments/doctor/unavailable` (`DOCTOR`)
- `DELETE /api/appointments/doctor/unavailable` (`DOCTOR`)

### Patients
- `POST /api/patients/register` (`RECEPTIONIST`, `DOCTOR`, `ADMIN`)
- `GET /api/patients`
- `GET /api/patients/{id}`
- `GET /api/patients/by-phone`
- `GET /api/patients/search`
- `PATCH /api/patients/{id}/medical-blob` (`DOCTOR`, `ADMIN`)

### Prescriptions
- `POST /api/prescriptions/create` (`DOCTOR`, `ADMIN`)
- `GET /api/prescriptions/pending` (`PHARMACIST`, `ADMIN`)
- `GET /api/prescriptions/latest` (`DOCTOR`, `ADMIN`)

### Pharmacy
- `GET /api/pharmacy/queue` (`PHARMACIST`, `ADMIN`)
- `POST /api/pharmacy/dispense/{id}` (`PHARMACIST`, `ADMIN`)
- `GET /api/pharmacy/inventory/alerts` (`PHARMACIST`, `ADMIN`)
- `GET /api/pharmacy/reports/daily-summary` (`ADMIN`)

### Medicines
- `GET /api/medicines/search` (`DOCTOR`, `PHARMACIST`, `ADMIN`)
- `GET /api/medicines` (`DOCTOR`, `PHARMACIST`, `ADMIN`)
- `POST /api/medicines` (`PHARMACIST`, `ADMIN`)
- `DELETE /api/medicines/expired/{id}` (`PHARMACIST`, `ADMIN`)
- `DELETE /api/medicines/expired` (`PHARMACIST`, `ADMIN`)

## 8. Security Model

- Stateless Spring Security session policy.
- JWT filter checks cookie first, then `Authorization` header.
- Role-based route protection in `SecurityConfig`.
- Additional controller method guards via `@PreAuthorize`.
- Passwords are stored as BCrypt hashes.
- Auth cookie is HTTP-only and `SameSite=Lax`.

## 9. Concurrency & Data Integrity

- Dispense flow is transactional (`@Transactional`).
- Medicine stock rows are locked with `PESSIMISTIC_WRITE` (`findByIdWithLock`).
- On insufficient stock, transaction rolls back (no partial deduction).
- Appointment booking enforces slot status conflicts + daily patient-doctor active rule.

## 10. Logging

- Debug request/method logging exists via filters/aspects and explicit `log.debug` calls.
- Effective behavior:
  - `logging.level.com.medid=INFO` (default): debug logs hidden
  - set to `DEBUG`: detailed operation logs visible

## 11. Setup & Run

## Prerequisites
- Java 21
- Node.js 18+
- PostgreSQL 14+

## Database credentials
Create `.env` in project root (or export env vars):

```env
DB_HOST=localhost
DB_PORT=5432
DB_NAME=medid_db
DB_USER=logi_admin
DB_PASSWORD=track_pass_2026
SPRING_PROFILES_ACTIVE=dev
```

`application.properties` imports `.env` and activates `dev` profile by default.

## Backend

```bash
./mvnw spring-boot:run
```

Backend default URL: `http://localhost:8080`

## Frontend

```bash
cd UI
npm install
npm run dev
```

Frontend default URL: `http://localhost:5173`

## Build checks

```bash
./mvnw -q -DskipTests compile
cd UI && npm run build
```

## 12. Project Structure

```text
.
├── src/main/java/com/medid
│   ├── config
│   ├── controller
│   ├── dto
│   ├── entity
│   ├── exception
│   ├── logging
│   ├── repository
│   ├── security
│   └── service
├── src/main/resources
│   ├── application.properties
│   ├── application-dev.properties
│   └── data.sql
└── UI
    ├── src
    │   ├── components
    │   ├── modules
    │   └── api.js
    └── package.json
```

## 13. Notes / Caveats

- `src/main/resources/data.sql` contains legacy inserts (including a `doctors` table) that may not match current entity model; treat it as optional sample data, not authoritative migration.
- JWT secret is currently hardcoded in `JwtUtils`; move to environment variable for production.
- `Appointment.doctorId` is scalar today; consider FK to `users` for stronger relational guarantees.

## 14. Recommended Next Improvements

- Introduce Flyway/Liquibase migrations for deterministic schema evolution.
- Move JWT secret/expiry and cookie security flags to environment config.
- Add integration tests for role authorization matrix and appointment conflict rules.
- Convert doctor scalar reference to proper relational mapping.
