# Medi-D Functionality Verification Steps

## Prerequisites
1. Start PostgreSQL
2. Run: `mvn spring-boot:run` (or use `--spring.profiles.active=dev`)
3. Login to get JWT: `POST /api/auth/login` with `{"username":"doctor","password":"doctorpass"}` (or receptionist/pharmacist)
4. Add header: `Authorization: Bearer <token>`

---

## Module A: Appointments

### Doctor marks slot as Unavailable
- **POST** `/api/appointments/mark-unavailable?doctorId=1&slotTime=2025-03-01T14:00:00`
- **Auth:** Doctor
- **Verify:** Slot cannot be booked by receptionist

### Receptionist books open slot
1. Check availability: **GET** `/api/appointments/check-available?doctorId=1&slotTime=2025-03-01T10:00:00` → should return `true`
2. Create patient: **POST** `/api/patients` with `{"name":"John","contact":"9999999999"}`
3. Book: **POST** `/api/appointments` with `{"patientId":1,"doctorId":1,"slotTimestamp":"2025-03-01T10:00:00","status":"BOOKED"}`
4. Try booking same slot again → should fail (slot not available)
5. Mark slot unavailable first, then try booking → should fail

---

## Module B: Consultation (Doctor enters diagnosis + medicines)

### Live medicine dropdown (DB fetch)
- **GET** `/api/medicines` → list all medicines
- **GET** `/api/medicines/search?pattern=para` → search medicines

### Doctor creates prescription with diagnosis
1. **POST** `/api/prescriptions` with `{"appointmentId":1,"diagnosis":"Common cold","status":"PENDING"}`
2. Add items: **POST** `/api/prescription-items` with `{"prescriptionId":1,"medicineId":1,"quantityRequired":5}`

---

## Module C: Pharmacy

### Pharmacist views pending prescriptions
- **GET** `/api/pharmacy/prescriptions/pending`
- **Auth:** Pharmacist
- **Response:** `[{prescription_id, items: [{medicine_id, medicine_name, quantity_required}]}]` — NO diagnosis, NO patient info

### Dispense reduces stock
1. Note medicine stock before: **GET** `/api/medicines/1`
2. **POST** `/api/pharmacy/prescriptions/1/dispense` (Pharmacist)
3. **GET** `/api/medicines/1` → stock reduced by prescribed quantity

---

## Module D: Privacy

### Doctor sees patient history
- **GET** `/api/appointments/patient/1` → appointments for patient
- **GET** `/api/prescriptions/patient/1` → prescriptions with diagnosis
- **Auth:** Doctor or Receptionist

### Pharmacist sees ONLY prescription queue (no diagnosis)
- **GET** `/api/pharmacy/prescriptions/pending` → medicine list only, no diagnosis
- **Verify:** Pharmacist gets 403 on **GET** `/api/prescriptions/1` or `/api/prescriptions/patient/1`

---

## Transactional Integrity

### Partial dispense not allowed
1. Create prescription with 3 items where one medicine has stock 0
2. **POST** `/api/pharmacy/prescriptions/{id}/dispense` → 409 Conflict
3. Verify no stock was deducted for any item (transaction rolled back)

---

## Quick Postman Collection Flow

1. **Login** (doctor) → save token
2. **Create patient**
3. **Create medicine** (stock 100)
4. **Doctor marks unavailable:** `POST /api/appointments/mark-unavailable?doctorId=1&slotTime=2025-03-15T14:00:00`
5. **Receptionist books:** `POST /api/appointments` (use slot not marked unavailable)
6. **Doctor creates prescription** with diagnosis + items
7. **Login** (pharmacist) → **GET** `/api/pharmacy/prescriptions/pending` → verify no diagnosis in response
8. **Dispense** → verify stock reduced
9. **Pharmacist** → **GET** `/api/prescriptions/1` → expect 403
