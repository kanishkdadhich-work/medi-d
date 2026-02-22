# MedID UI (React + Tailwind CSS)

Role-adaptive frontend for:
- ADMIN
- RECEPTIONIST
- DOCTOR
- PHARMACIST

## Run

```bash
cd UI
npm install
npm run dev
```

Frontend: `http://localhost:5173`  
Backend expected: `http://localhost:8080`

## What is implemented

- Authentication page with JWT/role storage in local state/localStorage.
- Role-based navigation (tabs are shown/hidden based on role).
- Receptionist Dashboard:
  - Daily appointments list
  - New Appointment modal (Full Name, Phone, Slot)
  - API uses `POST /api/appointments/create` and falls back to backend-compatible flow if not available.
- Doctor Dashboard:
  - Today's patient queue (SCHEDULED/BOOKED)
  - Consultation room with diagnosis notes
  - Dynamic medicine search and prescription item list
  - `POST /api/prescriptions/create`
- Pharmacist Dashboard:
  - Pending queue via `GET /api/prescriptions/pending`
  - Dispense via `POST /api/pharmacy/dispense/{id}`
  - Inventory alerts table (`stockCount <= minThreshold`)
  - Diagnosis notes are not rendered for privacy.
- Admin Dashboard:
  - Staff table with API/fallback cache
  - Create user form (`POST /api/admin/users/create`)

## Error handling

Errors use backend GlobalExceptionHandler style when available:
- `message`
- `error_code`

Shown in UI as `error_code: message`.
