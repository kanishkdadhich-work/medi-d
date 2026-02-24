# Medi-D

Medical and Inventory Management System with role-based workflows for:
- `ADMIN`
- `RECEPTIONIST`
- `DOCTOR`
- `PHARMACIST`

Backend: Spring Boot (`src/main/java`)  
Frontend: React + Vite + Tailwind (`UI/`)

## 1. Tech Stack

### Backend
- Java 21
- Spring Boot 3.5.x
- Spring Security (JWT, stateless)
- Spring Data JPA + Hibernate
- PostgreSQL
- Lombok

### Frontend
- React 18
- Vite 5
- Tailwind CSS

## 2. Architecture

- Frontend calls backend APIs under `/api/**`.
- Authentication is cookie-first JWT:
  - Access token cookie: `medid_token` (HTTP-only)
  - Refresh token cookie: `medid_refresh` (HTTP-only)
  - Fingerprint cookie: `medid_fp` (HTTP-only)
- Backend is stateless (`SessionCreationPolicy.STATELESS`).
- Authorization is role-based via `SecurityConfig` and `@PreAuthorize`.

## 3. Security Model

### Implemented hardening
- Short-lived access token + refresh token rotation.
- Fingerprint token binding:
  - JWT includes fingerprint (`fp`) and user-agent hash (`uah`) claims.
  - Request is authenticated only when token + fingerprint cookie + user-agent hash match.
- Logout token revocation:
  - Access and refresh tokens are revoked server-side on `/api/auth/logout`.
- Login brute-force mitigation:
  - `LoginRateLimitFilter` limits `/api/auth/login` to `20 req/sec` per IP.
- Browser-only API guard for authenticated sessions:
  - `ApiClientGuardFilter` blocks tool-style API calls (Postman/curl/etc.) for protected `/api/**` when auth material is present.

### Notes
- Passwords are stored as BCrypt hashes.
- Cookies are `SameSite=Lax`.
- `secure=false` in local/dev; set to `true` in HTTPS production.

## 4. Database Schema (Current)

Schema is entity-driven with `spring.jpa.hibernate.ddl-auto=update`.

### `users`
- `id` (PK)
- `username` (unique)
- `password` (BCrypt)
- `role`
- `doctor_id` (links to legacy `doctors.id` where applicable)
- `doctor_ref_code` (`MEDID-XX`, unique among doctors)
- `specialization`
- `weekday_shift` (`MORNING|EVENING|NIGHT`)
- `weekend_shift` (`MORNING|EVENING|NIGHT`)
- `enabled`

### `patients`
- `patient_id` (PK)
- `patient_ref_code` (unique public reference, format `PAT-XXXX`)
- `full_name`
- `phone_number` (unique)
- `gender`
- `medical_history_blob`
- `created_at`

### `appointments`
- `appointment_id` (PK)
- `patient_id` (FK to `patients.patient_id`, nullable for unavailable slots)
- `doctor_id`
- `appointment_time`
- `status` (`SCHEDULED|BOOKED|COMPLETED|CANCELLED|UNAVAILABLE`)

### `prescriptions`
- `prescription_id` (PK)
- `appointment_id` (FK)
- `diagnosis_notes`
- `status` (`PENDING|DISPENSED|CANCELLED`)
- `created_at`

### `prescription_items`
- `item_id` (PK)
- `prescription_id` (FK)
- `medicine_id` (FK)
- `quantity`

### `medicines`
- `medicine_id` (PK)
- `name`
- `stock_count`
- `min_threshold`
- `expiry_date`

## 5. Key Business Rules

### Appointment rules
- Booking allowed for `RECEPTIONIST`/`ADMIN`.
- Past slots are blocked in UI and validated server-side.
- Slot conflict statuses: `SCHEDULED`, `BOOKED`, `UNAVAILABLE`.
- `CANCELLED` and `COMPLETED` slots are reusable for future booking.
- A patient cannot hold multiple active appointments with the same doctor on the same day.
- Status transitions are restricted (including terminal behavior for `COMPLETED`/`CANCELLED`).
- Only doctor can manage `UNAVAILABLE` slot lifecycle.
- Patient selection in UI uses `patient_ref_code` instead of exposing DB primary key.

### Doctor profile rules
- For `DOCTOR`, required fields:
  - `specialization`
  - `doctor_ref_code` in `MEDID-XX` format
- Exactly one day type assignment:
  - weekday shift OR weekend shift (not both).
- Doctor calendar uses configured shift by selected date type:
  - weekday -> `weekday_shift`
  - weekend -> `weekend_shift`
  - shift windows: MORNING `06:00-14:00`, EVENING `14:00-22:00`, NIGHT `22:00-06:00`

### Prescription & pharmacy rules
- Prescription creation requires positive quantities.
- Completed appointments cannot be prescribed again.
- Pharmacist queue hides diagnosis notes.
- Dispense is transactional and concurrency-safe.
- Stock deduction uses FEFO (earliest-expiry-first by medicine name).
- Expired batches are excluded from dispensing and stock deduction.
- Duplicate medicine batch with same `name + expiry_date` is blocked; update stock instead.
- Expired batch deletion is blocked if referenced by prescription history (audit-safe conflict response).

## 6. API Summary

### Auth
- `POST /api/auth/login`
- `POST /api/auth/logout`
- `POST /api/auth/refresh`
- `GET /api/auth/me`
- `POST /api/auth/signup`

### Admin (`ADMIN`)
- `GET /api/admin/overview`
- `GET /api/admin/users`
- `GET /api/admin/users/paged`
- `POST /api/admin/users/create`
- `PUT /api/admin/users/{id}`
- `PATCH /api/admin/users/{id}/status`
- `DELETE /api/admin/users/{id}`

### Appointments
- `GET /api/appointments`
- `GET /api/appointments/paged`
- `POST /api/appointments/book`
- `POST /api/appointments/book-flex`
- `POST /api/appointments/{id}/cancel`
- `PATCH /api/appointments/{id}/status`
- `POST /api/appointments/{id}/complete`
- `GET /api/appointments/doctors`
- `GET /api/appointments/doctor/my`
- `GET /api/appointments/doctor/profile`
- `GET /api/appointments/doctor/today`
- `POST /api/appointments/doctor/unavailable`
- `DELETE /api/appointments/doctor/unavailable`

### Patients
- `POST /api/patients/register`
- `GET /api/patients`
- `GET /api/patients/paged`
- `GET /api/patients/{id}`
- `GET /api/patients/by-phone`
- `GET /api/patients/search`
- `PATCH /api/patients/{id}/medical-blob`

### Prescriptions
- `POST /api/prescriptions/create`
- `GET /api/prescriptions/pending`
- `GET /api/prescriptions/queue`
- `GET /api/prescriptions/queue/paged`
- `GET /api/prescriptions/latest`

### Pharmacy
- `GET /api/pharmacy/queue`
- `GET /api/pharmacy/queue/paged`
- `POST /api/pharmacy/dispense/{id}`
- `GET /api/pharmacy/inventory/alerts`
- `GET /api/pharmacy/reports/daily-summary`

### Medicines
- `GET /api/medicines`
- `GET /api/medicines/paged`
- `GET /api/medicines/search`
- `POST /api/medicines`
- `PUT /api/medicines/{id}/stock`
- `DELETE /api/medicines/expired/{id}`
- `DELETE /api/medicines/expired`

## 7. Frontend (UI) Highlights

- Role-based left navigation and module rendering.
- Receptionist booking flow:
  - create/select patient
  - specialization -> shift -> doctor
  - slot-level booking and status update modal
- Doctor workspace:
  - today/completed/future tabs
  - future tab is view-only (no workspace actions)
  - calendar view with unavailable marking, aligned to configured doctor shift
  - consultation and prescription drafting
- Pharmacist workspace:
  - pending queue
  - inventory alerts
  - all medicines + stock updates + expired deletion
  - add medicine
- Admin workspace:
  - user create/update/enable-disable/delete
  - doctor metadata management

## 8. Environment Configuration

Create `.env` in project root:

```env
DB_HOST=localhost
DB_PORT=5432
DB_NAME=medid_db
DB_USER=logi_admin
DB_PASSWORD=track_pass_2026

JWT_SECRET=change_this_to_a_long_random_secret
JWT_ACCESS_EXPIRATION_MS=1800000
JWT_REFRESH_EXPIRATION_MS=604800000

SPRING_PROFILES_ACTIVE=dev
DB_POOL_MAX=30
```

`application.properties` imports `.env` via:

```properties
spring.config.import=optional:file:.env[.properties]
```

## 9. Run

### Backend

```bash
./mvnw spring-boot:run
```

Backend default: `http://localhost:8080`

### Frontend

```bash
cd UI
npm install
npm run dev
```

Frontend default: `http://localhost:5173`

## 10. Verification Commands

```bash
./mvnw -q -DskipTests compile
cd UI && npm run build
```

## 11. Load/Resilience Testing Assets

Ready-to-use files:
- `postman/Medi-D-Resilience-Tests.postman_collection.json`
- `postman/TESTING_GUIDE.md`

Use these to validate:
- auth/role isolation
- JWT tampering rejection
- double-dispense race behavior
- stock rollback on insufficient inventory
- backend down handling and UI error paths

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
│   └── application-dev.properties
├── UI
│   ├── src
│   │   ├── components
│   │   ├── modules
│   │   └── api.js
│   └── package.json
└── postman
    ├── Medi-D-Resilience-Tests.postman_collection.json
    └── TESTING_GUIDE.md
```

## 13. Production Hardening Checklist

- Set `secure=true` for auth cookies under HTTPS.
- Set strong random `JWT_SECRET` via environment.
- Tune DB pool and JVM memory for expected concurrency.
- Add Flyway/Liquibase migrations for deterministic schema evolution.
- Add automated integration tests for status transitions and auth matrix.
