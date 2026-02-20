# Medi-D Postman Testing - Quick Reference Card

## 🚀 QUICK START (60 Seconds)

```bash
# Terminal 1: Start App
./mvnw spring-boot:run
# Wait for "Started MediDApplication"

# Terminal 2: Keep ready for next steps
```

In Postman:
1. **Import:** `medi-d-authenticated-postman-collection.json`
2. **Click:** "1️⃣ Login as Receptionist" → Send
3. **Click:** "Register New Patient" → Send
4. Done! You're authenticated and testing.

---

## 🔐 THE 3 TEST USERS (Copy-Paste Ready)

```
RECEPTIONIST
├─ Username: receptionist
├─ Password: receptpass
└─ Role: RECEPTIONIST

DOCTOR
├─ Username: doctor
├─ Password: doctorpass
└─ Role: DOCTOR

PHARMACIST
├─ Username: pharmacist
├─ Password: pharmacistpass
└─ Role: PHARMACIST
```

---

## 📧 Login Response Format

**Request:**
```json
POST /api/auth/login

{
  "username": "doctor",
  "password": "doctorpass"
}
```

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJkb2N0b3IiLCJyb2xlcyI6IlJPTEVfRE9DVE9SIiwiaWF0IjoxNzE4Njk5MzAwLCJleHAiOjE3MTg3MDI5MDB9.abc123..."
}
```

---

## 📊 ALL ENDPOINTS BY ROLE

### 🟢 RECEPTIONIST CAN ACCESS
```
✓ POST   /api/patients                         Create patient
✓ GET    /api/patients/{id}                    Get patient
✓ POST   /api/appointments                     Book appointment
✓ PUT    /api/appointments/{id}                Reschedule
✓ DELETE /api/appointments/{id}                Cancel
✓ GET    /api/appointments/patient/{id}        View patient appts

✗ Cannot: GET  /api/pharmacy/prescriptions/pending
✗ Cannot: POST /api/pharmacy/prescriptions/{id}/dispense
```

### 🔵 DOCTOR CAN ACCESS
```
✓ GET    /api/appointments/doctor/{id}        My appointments
✓ GET    /api/patients/{id}                    Patient history
✓ GET    /api/appointments/patient/{id}        Patient appts
✓ POST   /api/prescriptions                    Create prescription
✓ GET    /api/prescriptions                    My prescriptions
✓ GET    /api/medicines/search/by-name         Search medicines

✗ Cannot: POST /api/patients
✗ Cannot: POST /api/pharmacy/prescriptions/{id}/dispense
```

### 🟡 PHARMACIST CAN ACCESS
```
✓ GET    /api/pharmacy/prescriptions/pending   Pending queue
✓ POST   /api/pharmacy/prescriptions/{id}/dispense  Dispense
✓ GET    /api/medicines/{id}                   Check stock
✓ GET    /api/medicines/low-stock?min=50       Low stock
✓ GET    /api/medicines/expiring-before?date=X Expiring
✓ PUT    /api/medicines/{id}                   Update stock
✓ DELETE /api/medicines/{id}                   Delete medicine

✗ Cannot: POST /api/patients
✗ Cannot: POST /api/appointments
✗ Cannot: POST /api/prescriptions
✗ Cannot: See patient diagnosis (DTO filtered)
```

---

## 🎯 5-STEP TEST WORKFLOW

```
STEP 1: LOGIN (2 min)
├─ POST /api/auth/login (receptionist)
├─ POST /api/auth/login (doctor)
└─ POST /api/auth/login (pharmacist)

STEP 2: RECEPTIONIST TASKS (2 min)
├─ POST /api/patients (create patient)
└─ POST /api/appointments (book appointment)

STEP 3: DOCTOR TASKS (1 min)
├─ GET /api/appointments/doctor/10 (view mine)
└─ POST /api/prescriptions (create Rx)

STEP 4: PHARMACIST TASKS (1 min)
├─ GET /api/pharmacy/prescriptions/pending (view queue)
└─ POST /api/pharmacy/prescriptions/1/dispense (dispense)

STEP 5: SECURITY TESTS (1 min)
├─ Try without token → 401 Forbidden
├─ Try wrong role → 403 Forbidden
└─ Try invalid creds → 401 Unauthorized
```

---

## 🔧 POSTMAN HEADERS (All Protected Requests)

Every protected request needs:

```
Header Name:  Authorization
Header Value: Bearer {{receptionist_token}}
              Bearer {{doctor_token}}
              Bearer {{pharmacist_token}}
```

**The collection has this pre-configured!** Just login first.

---

## 📌 KEY RESPONSES

### Success (Do This)
```json
✓ 200 OK        - GET succeeded
✓ 201 Created   - POST succeeded
✓ 204 No Content- DELETE succeeded
```

### Errors (Debug These)
```json
✗ 401 Unauthorized        - No token or expired
✗ 403 Forbidden          - Wrong role for endpoint
✗ 400 Bad Request        - Invalid data
✗ 404 Not Found          - Resource doesn't exist
```

---

## 🎬 PRIVACY FEATURE (Pharmacist Queue)

**What Pharmacist Sees:**
```json
{
  "prescription_id": 1,
  "medicines": [
    {"name": "Aspirin", "quantity": 20}
  ]
}
```

**What Pharmacist Does NOT See:**
```json
{
  "patient_diagnosis": null,      ← HIDDEN
  "patient_history": null,        ← HIDDEN
  "doctor_notes": null            ← HIDDEN
}
```

This is enforced by `PharmacistPrescriptionDTO` - a filtered DTO.

---

## 🔄 ALL-OR-NOTHING DISPENSING

**Scenario A: All Medicines in Stock**
```
1. Check: Medicine 1 stock = 100 (need 20) ✓
2. Check: Medicine 2 stock = 50 (need 10) ✓
3. Dispense: Both medicines → SUCCESS
4. Update: Med 1: 100→80, Med 2: 50→40
```

**Scenario B: One Medicine Out of Stock**
```
1. Check: Medicine 1 stock = 100 (need 20) ✓
2. Check: Medicine 2 stock = 5 (need 10) ✗
3. Dispense: STOPPED
4. Update: NOTHING changes (rollback)
```

No partial dispensing! Either all or nothing.

---

## 🚨 HTTP STATUS CODES

| Code | Meaning | When |
|------|---------|------|
| 200 | OK | GET, PUT, POST succeeded |
| 201 | Created | POST created resource |
| 204 | No Content | DELETE succeeded |
| 400 | Bad Request | Invalid data sent |
| 401 | Unauthorized | No/invalid token |
| 403 | Forbidden | Wrong role for endpoint |
| 404 | Not Found | Resource doesn't exist |
| 500 | Server Error | App error |

---

## 🧪 SECURITY VERIFICATION TESTS

### Test 1: Receptionist Can't Register (Wrong Role)
```
POST /api/patients
Header: Authorization: Bearer {{receptionist_token}}
↓
✓ Expected: 403 Forbidden
✓ Message: Access Denied
```

### Test 2: No Token (Not Authenticated)
```
GET /api/patients/1
(No Authorization header)
↓
✓ Expected: 401 or 403 Unauthorized
```

### Test 3: Wrong Credentials (Bad Login)
```
POST /api/auth/login
{"username": "doctor", "password": "wrongpass"}
↓
✓ Expected: 401 Unauthorized
```

### Test 4: Doctor Can't Dispense (Wrong Role)
```
POST /api/pharmacy/prescriptions/1/dispense
Header: Authorization: Bearer {{doctor_token}}
↓
✓ Expected: 403 Forbidden
```

---

## 💡 POSTMAN TIPS

### Save Response as Example
1. Click response
2. Right-click → Save as example
3. Compare between roles

### Use Environment Variables
- `{{base_url}}` = `http://localhost:8080/api`
- `{{receptionist_token}}` = auto-filled
- `{{doctor_token}}` = auto-filled
- `{{pharmacist_token}}` = auto-filled

### Auto-Login with Pre-request Scripts
Add script to collection to auto-login every hour:
```javascript
if (token_expired) {
  // Post login request automatically
  // Save new token to environment
}
```

---

## 📋 TOKEN DETAILS

```
Type: JWT (JSON Web Token)
Format: Header.Payload.Signature

Header:
{
  "alg": "HS256",
  "typ": "JWT"
}

Payload:
{
  "sub": "doctor",
  "roles": "ROLE_DOCTOR",
  "iat": 1718699300,
  "exp": 1718702900
}

Signature: HMAC-SHA256 with secret key

Expiration: 1 hour (3600000 ms)
After expiration: Must re-login
```

---

## 🎓 REAL-WORLD WORKFLOW EXAMPLE

### Patient Getting Medicine (End-to-End)

```
1️⃣ RECEPTIONIST
   └─ POST /api/patients {"name": "John", "contact": "+1-555-0100"}
   └─ Token: {{receptionist_token}}
   └─ Response: {"id": 1}

2️⃣ RECEPTIONIST
   └─ POST /api/appointments {"patientId": 1, "doctorId": 10, "slotTimestamp": "..."}
   └─ Token: {{receptionist_token}}
   └─ Response: {"appointment_id": 1}

3️⃣ DOCTOR (after appointment)
   └─ POST /api/prescriptions {"patientId": 1, "doctorId": 10, "prescriptionDate": "..."}
   └─ Token: {{doctor_token}}
   └─ Response: {"prescription_id": 1}

4️⃣ PHARMACIST
   └─ GET /api/pharmacy/prescriptions/pending
   └─ Token: {{pharmacist_token}}
   └─ Response: [{"prescription_id": 1, "medicines": [...]}]

5️⃣ PHARMACIST
   └─ POST /api/pharmacy/prescriptions/1/dispense
   └─ Token: {{pharmacist_token}}
   └─ Response: 200 OK (medicines dispensed)
```

---

## ⏱️ TOKEN LIFECYCLE

```
MINUTE 0:   Login → Get token (expires at minute 60)
MINUTE 30:  Use token → Still valid
MINUTE 59:  Use token → Still valid
MINUTE 60:  Use token → INVALID (expired)
MINUTE 61:  → Must re-login
```

---

## 🔒 SECURITY STATUS

| Feature | Status | Verified? |
|---------|--------|-----------|
| JWT Tokens | ✅ Working | Test login |
| Role Enforcement | ✅ Working | Test 403 errors |
| Password Encryption | ✅ Working | BCrypt used |
| Token Expiration | ✅ Working | Test after 1 hour |
| Rate Limiting | ✅ Working | Rapid requests throttled |
| CORS Protection | ✅ Working | Cross-origin blocked |

---

## 📞 NEED HELP?

| Issue | Solution |
|-------|----------|
| "401 Unauthorized" | Login first (POST /api/auth/login) |
| "403 Forbidden" | Wrong role for this endpoint |
| "Connection refused" | Start app: `./mvnw spring-boot:run` |
| "Invalid credentials" | Check username/password spelling |
| "Token expired" | Re-login to get new token |

---

## 🎯 SUCCESS INDICATORS

You'll know it's working when:
✅ Login returns token
✅ Token saved to environment variable  
✅ Protected endpoints return 200/201 with token
✅ Pharmacist sees medicines but not diagnosis
✅ Receptionist can't dispense (403)
✅ Doctor can't register patient (403)
✅ Without token = 401/403

---

## 📚 DETAILED GUIDES

Need more info? Read these:
1. **POSTMAN_AUTH_QUICK_START.md** - Step-by-step setup
2. **ROLE_BASED_TESTING_GUIDE.md** - Complete reference
3. **COMPLETE_ROLE_BASED_SUMMARY.md** - Full system status

---

## 🚀 YOU'RE READY!

Import the collection and start testing:
```
File → Import → medi-d-authenticated-postman-collection.json
```

Let's go! 🎉
