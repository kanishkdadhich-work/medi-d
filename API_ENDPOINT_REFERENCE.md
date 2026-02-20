# 📚 Medi-D API - Complete Endpoint Reference

## 🔑 Legend
- ✅ = No auth required
- 🔓 = Auth required (any role)
- 🔐 = Auth required + specific role only
- | = OR condition

---

## 📊 ALL ENDPOINTS QUICK REFERENCE

### Public Endpoints (No Token Needed)
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/health` | ✅ | System health check |
| POST | `/api/auth/login` | ✅ | Login and get JWT token |

---

### Patient Management
| Method | Endpoint | Auth | Role(s) | Description |
|--------|----------|------|---------|-------------|
| POST | `/api/patients` | 🔓 | Any | Create new patient |
| GET | `/api/patients/{id}` | 🔓 | Any | Get patient by ID |

**Notes:**
- Currently ANY authenticated user can create patients
- **RECOMMENDED:** Only RECEPTIONIST should create patients

---

### Appointment Management
| Method | Endpoint | Auth | Role(s) | Description |
|--------|----------|------|---------|-------------|
| POST | `/api/appointments` | 🔓 | Any | Create appointment |
| GET | `/api/appointments/{id}` | 🔓 | Any | Get appointment by ID |
| PUT | `/api/appointments/{id}` | 🔓 | Any | Update appointment |
| DELETE | `/api/appointments/{id}` | 🔓 | Any | Delete appointment |
| GET | `/api/appointments/doctor/{doctorId}` | 🔓 | Any | Get doctor's appointments |
| GET | `/api/appointments/patient/{patientId}` | 🔐 | DOCTOR \| RECEPTIONIST | Get patient's appointments |
| GET | `/api/appointments/search/between` | 🔓 | Any | Search appointments by time range |
| GET | `/api/appointments/booked/{doctorId}` | 🔓 | Any | Get booked slots for doctor |
| GET | `/api/appointments/check-slot` | 🔓 | Any | Check if slot is available |
| GET | `/api/appointments/check-available` | 🔓 | Any | Check if slot is available |
| GET | `/api/appointments/by-status/{status}` | 🔓 | Any | Get appointments by status |
| POST | `/api/appointments/mark-unavailable` | 🔐 | DOCTOR | Mark slot as unavailable |

**Notes:**
- Most endpoints are public (any authenticated user)
- Only `/api/appointments/patient/{id}` requires DOCTOR or RECEPTIONIST
- Only `/api/appointments/mark-unavailable` requires DOCTOR
- **RECOMMENDED:** Only RECEPTIONIST should create appointments

---

### Medicine Management
| Method | Endpoint | Auth | Role(s) | Description |
|--------|----------|------|---------|-------------|
| GET | `/api/medicines` | 🔓 | Any | Get all medicines |
| GET | `/api/medicines/{id}` | 🔓 | Any | Get medicine by ID |
| GET | `/api/medicines/search/by-name` | 🔓 | Any | Search medicine by name |
| POST | `/api/medicines` | 🔓 | Any | Create new medicine |
| PUT | `/api/medicines/{id}` | 🔓 | Any | Update medicine |
| DELETE | `/api/medicines/{id}` | 🔓 | Any | Delete medicine |
| GET | `/api/medicines/expiring-before` | 🔓 | Any | Get medicines expiring before date |
| GET | `/api/medicines/low-stock` | 🔓 | Any | Get low stock medicines |
| GET | `/api/medicines/search` | 🔓 | Any | Search medicines by pattern |

**Notes:**
- Currently ANY authenticated user can manage medicines
- **RECOMMENDED:** Only PHARMACIST should create/update/delete medicines

---

### Prescription Management
| Method | Endpoint | Auth | Role(s) | Description |
|--------|----------|------|---------|-------------|
| POST | `/api/prescriptions` | 🔐 | DOCTOR \| RECEPTIONIST | Create prescription |
| GET | `/api/prescriptions/{id}` | 🔐 | DOCTOR \| RECEPTIONIST | Get prescription by ID |
| GET | `/api/prescriptions/appointment/{appointmentId}` | 🔐 | DOCTOR \| RECEPTIONIST | Get prescription for appointment |
| PUT | `/api/prescriptions/{id}` | 🔐 | DOCTOR \| RECEPTIONIST | Update prescription |
| DELETE | `/api/prescriptions/{id}` | 🔐 | DOCTOR \| RECEPTIONIST | Delete prescription |
| GET | `/api/prescriptions/pending` | 🔐 | DOCTOR \| RECEPTIONIST | Get pending prescriptions |
| GET | `/api/prescriptions/dispensed` | 🔐 | DOCTOR \| RECEPTIONIST | Get dispensed prescriptions |
| GET | `/api/prescriptions/by-status/{status}` | 🔐 | DOCTOR \| RECEPTIONIST | Get by status |
| GET | `/api/prescriptions/patient/{patientId}` | 🔐 | DOCTOR \| RECEPTIONIST | Get patient's prescriptions |
| GET | `/api/prescriptions/patient/{patientId}/pending-count` | 🔐 | DOCTOR \| RECEPTIONIST | Count pending for patient |
| POST | `/api/prescriptions/{id}/dispense` | 🔐 | DOCTOR \| RECEPTIONIST | Dispense prescription |

**Notes:**
- **ISSUE:** Both DOCTOR and RECEPTIONIST can create prescriptions
- **RECOMMENDED:** Only DOCTOR should create prescriptions (POST)
- **ISSUE:** `/api/prescriptions/{id}/dispense` allows DOCTOR/RECEPTIONIST
- **RECOMMENDED:** Use `/api/pharmacy/prescriptions/{id}/dispense` instead (PHARMACIST only)

---

### Prescription Items
| Method | Endpoint | Auth | Role(s) | Description |
|--------|----------|------|---------|-------------|
| POST | `/api/prescription-items` | 🔓 | Any | Create prescription item |
| GET | `/api/prescription-items/{id}` | 🔓 | Any | Get prescription item by ID |
| PUT | `/api/prescription-items/{id}` | 🔓 | Any | Update prescription item |
| DELETE | `/api/prescription-items/{id}` | 🔓 | Any | Delete prescription item |
| GET | `/api/prescription-items/prescription/{prescriptionId}` | 🔓 | Any | Get items for prescription |
| GET | `/api/prescription-items/medicine/{medicineId}` | 🔓 | Any | Get items for medicine |
| GET | `/api/prescription-items/medicine/{medicineId}/pending` | 🔓 | Any | Get pending items for medicine |

**Notes:**
- Currently ANY authenticated user can manage items
- Items are typically managed through prescription creation

---

### Pharmacy Management (Pharmacist Only)
| Method | Endpoint | Auth | Role(s) | Description |
|--------|----------|------|---------|-------------|
| GET | `/api/pharmacy/prescriptions/pending` | 🔐 | PHARMACIST | Get pending prescriptions (privacy-filtered) |
| POST | `/api/pharmacy/prescriptions/{id}/dispense` | 🔐 | PHARMACIST | Dispense prescription (all-or-nothing) |

**Notes:**
- Only endpoint that properly restricts to PHARMACIST
- Response filters out diagnosis and patient history
- All-or-nothing: if any medicine is out of stock, entire operation fails

---

## 🎯 TEST USERS

```
Username: receptionist     | Username: doctor         | Username: pharmacist
Password: receptpass       | Password: doctorpass     | Password: pharmacistpass
Role: RECEPTIONIST         | Role: DOCTOR             | Role: PHARMACIST
```

---

## 🔐 AUTHENTICATION FLOW

```
1. Login
   POST /api/auth/login
   Body: {"username": "doctor", "password": "doctorpass"}
   ↓
   Response: {"token": "eyJhbGciOiJIUzI1NiIs..."}

2. Use Token
   GET /api/patients/1
   Header: Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
   ↓
   Response: {"id": 1, "name": "John Smith"}

3. Token Expires After
   1 hour (3600 seconds) from creation
   ↓
   Need to login again to get fresh token
```

---

## 📝 REQUEST BODY EXAMPLES

### Login
```json
POST /api/auth/login
{
  "username": "doctor",
  "password": "doctorpass"
}
```

### Create Patient
```json
POST /api/patients
{
  "name": "John Smith",
  "contact": "+1-555-0100"
}
```

### Create Appointment
```json
POST /api/appointments
{
  "patientId": 1,
  "doctorId": 10,
  "slotTimestamp": "2026-03-20T10:00:00"
}
```

### Create Prescription
```json
POST /api/prescriptions
{
  "appointmentId": 1,
  "prescriptionDate": "2026-03-20"
}
```

### Create Medicine
```json
POST /api/medicines
{
  "name": "Aspirin",
  "stock": 100,
  "expiryDate": "2027-12-31"
}
```

### Dispense Prescription
```
POST /api/pharmacy/prescriptions/1/dispense
(No body needed)
```

---

## ⚠️ ISSUES FOUND & RECOMMENDATIONS

### Issue 1: Missing Role Restrictions

| Endpoint | Current | Should Be |
|----------|---------|-----------|
| POST /api/patients | Any | 🔐 RECEPTIONIST |
| POST /api/appointments | Any | 🔐 RECEPTIONIST |
| POST /api/medicines | Any | 🔐 PHARMACIST |
| PUT /api/medicines | Any | 🔐 PHARMACIST |
| DELETE /api/medicines | Any | 🔐 PHARMACIST |

### Issue 2: Wrong Role Restrictions

| Endpoint | Current | Should Be |
|----------|---------|-----------|
| POST /api/prescriptions | 🔐 DOCTOR \| RECEPTIONIST | 🔐 DOCTOR only |
| POST /api/prescriptions/{id}/dispense | 🔐 DOCTOR \| RECEPTIONIST | ❌ Remove (use pharmacy endpoint) |

### Issue 3: Dual Dispensing Endpoints

| Endpoint | Role | Issue |
|----------|------|-------|
| POST /api/prescriptions/{id}/dispense | DOCTOR \| RECEPTIONIST | Allows wrong roles |
| POST /api/pharmacy/prescriptions/{id}/dispense | PHARMACIST | Correct endpoint |

**Solution:** Remove `/api/prescriptions/{id}/dispense` and ONLY use `/api/pharmacy/prescriptions/{id}/dispense`

---

## 🧪 TESTING GUIDE BY ROLE

### Test Receptionist (Patient Registration & Appointments)
```
1. Login: POST /api/auth/login (receptionist/receptpass)
2. Create Patient: POST /api/patients
3. Create Appointment: POST /api/appointments
4. View Patient Appointments: GET /api/appointments/patient/{id}
   Expected: 200 OK (RECEPTIONIST has access)
5. Try Doctor Endpoint: GET /api/appointments/mark-unavailable
   Expected: 403 Forbidden (not a DOCTOR)
```

### Test Doctor (Queue & Prescriptions)
```
1. Login: POST /api/auth/login (doctor/doctorpass)
2. View Appointments: GET /api/appointments/doctor/{id}
3. View Patient Details: GET /api/patients/{id}
4. Create Prescription: POST /api/prescriptions
5. Mark Unavailable: POST /api/appointments/mark-unavailable
   Expected: 200 OK (DOCTOR has access)
6. Try Pharmacy Endpoint: GET /api/pharmacy/prescriptions/pending
   Expected: 403 Forbidden (not a PHARMACIST)
```

### Test Pharmacist (Dispensing & Inventory)
```
1. Login: POST /api/auth/login (pharmacist/pharmacistpass)
2. View Pending: GET /api/pharmacy/prescriptions/pending
   Expected: 200 OK, no diagnosis shown
3. Dispense: POST /api/pharmacy/prescriptions/{id}/dispense
4. View Medicines: GET /api/medicines
5. Low Stock: GET /api/medicines/low-stock?minimumStock=50
6. Try Doctor Endpoint: GET /api/appointments/patient/{id}
   Expected: 403 Forbidden (not a DOCTOR/RECEPTIONIST)
```

---

## ✅ POSTMAN IMPORT STEPS

1. **Download Collection:** `medi-d-postman-collection-FIXED.json`
2. **Import:** Postman → File → Import → Select JSON file
3. **Environment:** Create environment with token variables
4. **Login:** Run authentication requests in order
5. **Test:** Run requests for each role

---

## 🆘 QUICK FIXES FOR COMMON ERRORS

| Error | Cause | Fix |
|-------|-------|-----|
| 403 Forbidden | No Bearer token | Add `Authorization: Bearer <token>` header |
| 401 Unauthorized | Invalid/expired token | Login again to get fresh token |
| 403 Forbidden | Wrong role | Use token from correct role's login |
| 400 Bad Request | Invalid JSON body | Check request body format and required fields |
| 404 Not Found | Resource doesn't exist | Create resource first or verify ID |
| 500 Server Error | Backend issue | Check server logs, restart app |

