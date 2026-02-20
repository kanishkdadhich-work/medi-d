# 🔒 Medi-D API Security & Access Control Audit

## CRITICAL FINDINGS

### 1. SecurityConfig Requirements
**File:** `SecurityConfig.java`

**IMPORTANT:** The SecurityConfig enforces that:
- ✅ `/api/auth/**` → Public (login)
- ✅ `/api/health` → Public (health check)
- ✅ `/actuator/**` → Public
- ✅ `/error` → Public
- ❌ **ALL OTHER ENDPOINTS → REQUIRE VALID JWT TOKEN**

**This means:** Even if an endpoint has NO @PreAuthorize annotation, it STILL REQUIRES a Bearer token!

---

## 2. Test Users (Pre-configured In-Memory)

```java
Username: receptionist
Password: receptpass
Role: RECEPTIONIST
────────────────────────
Username: doctor
Password: doctorpass
Role: DOCTOR
────────────────────────
Username: pharmacist
Password: pharmacistpass
Role: PHARMACIST
```

---

## 3. COMPLETE ENDPOINT & ROLE MATRIX

### Public Endpoints (No Token Required)
```
✅ GET    /api/health                              NO AUTH REQUIRED
✅ POST   /api/auth/login                         NO AUTH REQUIRED
```

---

### Protected Endpoints (Token REQUIRED - But ANY Authenticated User)

#### Patient Management
```
🔓 GET    /api/patients/{id}                      ANY AUTHENTICATED
🔓 POST   /api/patients                           ANY AUTHENTICATED
```

#### Medicine Management (All endpoints)
```
🔓 GET    /api/medicines                          ANY AUTHENTICATED
🔓 GET    /api/medicines/{id}                     ANY AUTHENTICATED
🔓 GET    /api/medicines/search/by-name           ANY AUTHENTICATED
🔓 POST   /api/medicines                          ANY AUTHENTICATED
🔓 PUT    /api/medicines/{id}                     ANY AUTHENTICATED
🔓 GET    /api/medicines/expiring-before          ANY AUTHENTICATED
🔓 GET    /api/medicines/low-stock                ANY AUTHENTICATED
🔓 GET    /api/medicines/search                   ANY AUTHENTICATED
🔓 DELETE /api/medicines/{id}                     ANY AUTHENTICATED
```

#### Appointment Management
```
🔓 GET    /api/appointments/{id}                  ANY AUTHENTICATED
🔓 POST   /api/appointments                       ANY AUTHENTICATED
🔓 PUT    /api/appointments/{id}                  ANY AUTHENTICATED
🔓 GET    /api/appointments/doctor/{doctorId}    ANY AUTHENTICATED
🔓 GET    /api/appointments/search/between        ANY AUTHENTICATED
🔓 GET    /api/appointments/booked/{doctorId}    ANY AUTHENTICATED
🔓 GET    /api/appointments/check-slot            ANY AUTHENTICATED
🔓 GET    /api/appointments/check-available       ANY AUTHENTICATED
🔓 GET    /api/appointments/by-status/{status}   ANY AUTHENTICATED
🔓 DELETE /api/appointments/{id}                  ANY AUTHENTICATED

🔐 GET    /api/appointments/patient/{patientId}  DOCTOR or RECEPTIONIST
🔐 POST   /api/appointments/mark-unavailable     DOCTOR ONLY
```

#### Prescription Management
```
⚠️ CLASS LEVEL: @PreAuthorize("hasRole('DOCTOR') or hasRole('RECEPTIONIST')")
   This means ALL endpoints below require DOCTOR or RECEPTIONIST role:

🔐 GET    /api/prescriptions/{id}                DOCTOR or RECEPTIONIST
🔐 GET    /api/prescriptions/appointment/{id}   DOCTOR or RECEPTIONIST
🔐 POST   /api/prescriptions                     DOCTOR or RECEPTIONIST
🔐 PUT    /api/prescriptions/{id}               DOCTOR or RECEPTIONIST
🔐 GET    /api/prescriptions/by-status/{status} DOCTOR or RECEPTIONIST
🔐 GET    /api/prescriptions/pending             DOCTOR or RECEPTIONIST
🔐 GET    /api/prescriptions/dispensed           DOCTOR or RECEPTIONIST
🔐 GET    /api/prescriptions/patient/{id}       DOCTOR or RECEPTIONIST
🔐 GET    /api/prescriptions/patient/{id}/pending-count  DOCTOR or RECEPTIONIST
🔐 DELETE /api/prescriptions/{id}               DOCTOR or RECEPTIONIST
🔐 POST   /api/prescriptions/{id}/dispense      DOCTOR or RECEPTIONIST (⚠️ ISSUE!)
```

#### Prescription Items (All endpoints require auth, no specific role)
```
🔓 GET    /api/prescription-items/{id}           ANY AUTHENTICATED
🔓 POST   /api/prescription-items                ANY AUTHENTICATED
🔓 PUT    /api/prescription-items/{id}           ANY AUTHENTICATED
🔓 GET    /api/prescription-items/prescription/{id}  ANY AUTHENTICATED
🔓 GET    /api/prescription-items/medicine/{id}    ANY AUTHENTICATED
🔓 GET    /api/prescription-items/medicine/{id}/pending  ANY AUTHENTICATED
🔓 DELETE /api/prescription-items/{id}           ANY AUTHENTICATED
```

#### Pharmacy Management (Pharmacist Only)
```
🔐 GET    /api/pharmacy/prescriptions/pending    PHARMACIST ONLY
🔐 POST   /api/pharmacy/prescriptions/{id}/dispense  PHARMACIST ONLY
```

---

## 4. CRITICAL ISSUES FOUND

### ⚠️ Issue #1: PrescriptionController Dispense Endpoint
**Problem:**
```
❌ POST /api/prescriptions/{id}/dispense
   Requires: DOCTOR or RECEPTIONIST
   
✅ POST /api/pharmacy/prescriptions/{id}/dispense
   Requires: PHARMACIST
```

**Why It's Wrong:**
- Only PHARMACIST should dispense medications (from `/api/pharmacy/prescriptions/{id}/dispense`)
- The `/api/prescriptions/{id}/dispense` endpoint in PrescriptionController should NOT exist or should be restricted to DOCTOR/RECEPTIONIST for a different purpose
- This creates confusion and dual endpoints for same operation

**Recommendation:**
- Use `/api/pharmacy/prescriptions/{id}/dispense` (PHARMACIST only) for actual dispensing
- Remove or repurpose `/api/prescriptions/{id}/dispense`

---

### ⚠️ Issue #2: Receptionist Can Create Prescriptions
**Problem:**
```
@PreAuthorize("hasRole('DOCTOR') or hasRole('RECEPTIONIST')")
public class PrescriptionController
```

**Why It's Wrong:**
- Prescriptions should ONLY be created by DOCTORS
- Receptionists should NOT be able to create prescriptions
- Receptionists can VIEW/RETRIEVE prescriptions but not CREATE them

**Recommendation:**
- Change POST /api/prescriptions to `@PreAuthorize("hasRole('DOCTOR')")`
- Keep GET/READ operations as `hasRole('DOCTOR') or hasRole('RECEPTIONIST')`

---

### ⚠️ Issue #3: No Role Protection on Patient Registration
**Problem:**
```
POST /api/patients
   Currently: ANY AUTHENTICATED USER can register patients
   Should be: RECEPTIONIST ONLY
```

**Recommendation:**
- Add `@PreAuthorize("hasRole('RECEPTIONIST')")` to POST /api/patients

---

### ⚠️ Issue #4: No Role Protection on Medicines
**Problem:**
```
POST/PUT/DELETE /api/medicines
   Currently: ANY AUTHENTICATED USER can manage medicines
   Should be: PHARMACIST ONLY
```

**Recommendation:**
- Add `@PreAuthorize("hasRole('PHARMACIST')")` to POST/PUT/DELETE medicine endpoints

---

### ⚠️ Issue #5: No Role Protection on Appointments (Create)
**Problem:**
```
POST /api/appointments
   Currently: ANY AUTHENTICATED USER can book appointments
   Should be: RECEPTIONIST ONLY
```

**Recommendation:**
- Add `@PreAuthorize("hasRole('RECEPTIONIST')")` to POST /api/appointments

---

## 5. RECOMMENDED FINAL ACCESS CONTROL MATRIX

| Endpoint | HTTP | Receptionist | Doctor | Pharmacist | Auth Required |
|----------|------|:------------:|:------:|:----------:|:-------------:|
| **Auth** |
| /api/auth/login | POST | ✅ | ✅ | ✅ | ❌ |
| /api/health | GET | ✅ | ✅ | ✅ | ❌ |
| **Patients** |
| /api/patients (create) | POST | ✅ | ❌ | ❌ | ✅ |
| /api/patients/{id} (read) | GET | ✅ | ✅ | ❌ | ✅ |
| **Medicines** |
| /api/medicines (read) | GET | ✅ | ✅ | ✅ | ✅ |
| /api/medicines (create/update/delete) | POST/PUT/DELETE | ❌ | ❌ | ✅ | ✅ |
| /api/medicines/low-stock | GET | ❌ | ❌ | ✅ | ✅ |
| /api/medicines/expiring-before | GET | ❌ | ❌ | ✅ | ✅ |
| **Appointments** |
| /api/appointments (create) | POST | ✅ | ❌ | ❌ | ✅ |
| /api/appointments (read/update/delete) | GET/PUT/DELETE | ✅ | ✅ | ❌ | ✅ |
| /api/appointments/mark-unavailable | POST | ❌ | ✅ | ❌ | ✅ |
| **Prescriptions** |
| /api/prescriptions (create) | POST | ❌ | ✅ | ❌ | ✅ |
| /api/prescriptions (read/update/delete) | GET/PUT/DELETE | ✅ | ✅ | ❌ | ✅ |
| **Pharmacy** |
| /api/pharmacy/prescriptions/pending | GET | ❌ | ❌ | ✅ | ✅ |
| /api/pharmacy/prescriptions/{id}/dispense | POST | ❌ | ❌ | ✅ | ✅ |

---

## 6. WHY YOU'RE GETTING "FORBIDDEN" ERRORS

### Scenario A: No Bearer Token
```
GET /api/patients/1
Headers: (NONE)

Response: 401 or 403 Unauthorized
Reason: SecurityConfig requires authentication for all endpoints except /api/auth/**, /api/health, /actuator/**, /error
```

### Scenario B: Expired or Invalid Token
```
GET /api/patients/1
Headers: Authorization: Bearer eyJhbGciOiJIUzI1NiIs... (invalid/expired)

Response: 403 Forbidden
Reason: JwtAuthenticationFilter validates the token, and if invalid/expired, it rejects the request
```

### Scenario C: Insufficient Role
```
GET /api/appointments/patient/1
Headers: Authorization: Bearer [PHARMACIST_TOKEN]

Response: 403 Forbidden
Reason: @PreAuthorize requires DOCTOR or RECEPTIONIST, but PHARMACIST token doesn't have that role
```

---

## 7. HOW TO FIX "FORBIDDEN" ERRORS IN POSTMAN

### Step 1: Verify You're Sending Bearer Token
Every request (except login and health) needs:
```
Authorization: Bearer <your_jwt_token>
```

### Step 2: Verify Token is Valid
- Token should be extracted from POST /api/auth/login response
- Token should be set in environment variable (e.g., {{token}})
- Token should NOT be expired (1 hour expiration by default)

### Step 3: Verify Token Has Right Role
- If accessing DOCTOR endpoint  → Login as doctor
- If accessing RECEPTIONIST endpoint → Login as receptionist
- If accessing PHARMACIST endpoint → Login as pharmacist

### Step 4: Verify Bearer Format
```
✅ CORRECT: Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
❌ WRONG:   Authorization: eyJhbGciOiJIUzI1NiIs...
❌ WRONG:   Authorization: Token eyJhbGciOiJIUzI1NiIs...
```

---

## 8. POSTMAN WORKFLOW TO TEST

```
1. Login as different role:
   POST /api/auth/login
   Body: {"username": "doctor", "password": "doctorpass"}
   Response: {"token": "eyJhbGciOiJIUzI1NiIs..."}
   Save token to {{doctor_token}}

2. Use token in subsequent requests:
   GET /api/appointments/doctor/10
   Headers: Authorization: Bearer {{doctor_token}}
   Response: 200 OK (if doctor has appointments)

3. Test forbidden access:
   GET /api/pharmacy/prescriptions/pending
   Headers: Authorization: Bearer {{doctor_token}}
   Response: 403 Forbidden (DOCTOR doesn't have PHARMACIST role)

4. Switch to pharmacist token:
   POST /api/auth/login
   Body: {"username": "pharmacist", "password": "pharmacistpass"}
   Save token to {{pharmacist_token}}

5. Try again:
   GET /api/pharmacy/prescriptions/pending
   Headers: Authorization: Bearer {{pharmacist_token}}
   Response: 200 OK (PHARMACIST has access)
```

---

## 9. SUMMARY TABLE: "FORBIDDEN" CAUSES & SOLUTIONS

| Cause | Example | Solution |
|-------|---------|----------|
| No Bearer token | `GET /api/patients/1` without `Authorization` header | Add `Authorization: Bearer <token>` header |
| Expired token | Token generated 2+ hours ago | Login again to get fresh token |
| Invalid token | Malformed or tampered token | Login again, don't manually edit token |
| Wrong role | PHARMACIST accessing DOCTOR endpoint | Login as correct role |
| Token in request body | Putting token in request body instead of header | Use `Authorization` header only |
| CORS issue | Postman sending OPTIONS request | Usually not an issue with Postman, check WebConfig |
| Typo in header name | `Authorizaton: Bearer <token>` (missing 'z') | Use exact spelling: `Authorization` |

---

## 10. CHECKLIST FOR TESTING

- [ ] Verify app is running: `./mvnw spring-boot:run`
- [ ] Verify database is working: `./test-db-connection.sh`
- [ ] Login as receptionist, get token
- [ ] Save token to {{receptionist_token}} environment variable
- [ ] Test endpoint with token: `Authorization: Bearer {{receptionist_token}}`
- [ ] Verify response is 200 OK (not 403)
- [ ] Repeat for doctor and pharmacist tokens
- [ ] Test forbidden access: use wrong role token, should get 403
- [ ] Test without token: should get 401/403

