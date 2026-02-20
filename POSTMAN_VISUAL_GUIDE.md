# Medi-D Postman Testing - Visual Flow Guide

## System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Your Postman Client                      │
└────────────────────────┬────────────────────────────────────┘
                         │
                    HTTP Requests
                         │
                         ↓
┌─────────────────────────────────────────────────────────────┐
│          Medi-D Spring Boot REST API                        │
│         (http://localhost:8080/api)                         │
│                                                             │
│  ┌─────────────┐  ┌──────────────┐  ┌─────────────────┐  │
│  │  Patients   │  │  Medicines   │  │  Appointments   │  │
│  │  Endpoint   │  │  Endpoint    │  │  Endpoint       │  │
│  └─────────────┘  └──────────────┘  └─────────────────┘  │
│  ┌─────────────┐                                           │
│  │ Prescriptions│                                          │
│  │  Endpoint   │                                           │
│  └─────────────┘                                           │
└────────────────────────┬────────────────────────────────────┘
                         │
                    Database Query
                         │
                         ↓
┌─────────────────────────────────────────────────────────────┐
│              PostgreSQL Database                            │
│         (medid_test / medid_admin)                         │
│                                                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  │
│  │ Patients │  │Medicines │  │  Appts   │  │ Prescrs  │  │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘  │
└─────────────────────────────────────────────────────────────┘
```

---

## Complete Testing Workflow

```
START: Postman Opened
   │
   ├─→ 1. Import Collection
   │      medi-d-postman-collection.json
   │
   ├─→ 2. Set Environment Variables
   │      base_url = http://localhost:8080/api
   │
   ├─→ 3. HEALTH CHECK
   │   └─→ [GET] /health
   │       ✓ Status 200
   │       ✓ Response: "Medi-D System is up and running!"
   │
   ├─→ 4. PATIENT MANAGEMENT
   │   ├─→ [POST] /patients (Create Patient)
   │   │   Request:  {"name": "John Doe", "contact": "+1-555-0100"}
   │   │   Response: {"id": 1, "name": "John Doe"}  [201]
   │   │   → Auto-saves: patient_id = 1
   │   │
   │   ├─→ [GET] /patients/1 (Get Patient)
   │   │   Response: {"id": 1, "name": "John Doe"}  [200]
   │   │
   │   └─→ [GET] /patients/999 (Test 404)
   │       Response: Error message  [404]
   │
   ├─→ 5. MEDICINE MANAGEMENT
   │   ├─→ [POST] /medicines (Create Medicine)
   │   │   Request:  {"name": "Aspirin", "stock": 100, "expiryDate": "2025-12-31"}
   │   │   Response: {"medicine_id": 1, ...}  [201]
   │   │   → Auto-saves: medicine_id = 1
   │   │
   │   ├─→ [GET] /medicines/1 (Get Medicine)
   │   │   Response: {"medicine_id": 1, "medicine_name": "Aspirin", ...}  [200]
   │   │
   │   ├─→ [GET] /medicines/search/by-name?name=Aspirin (Search)
   │   │   Response: {...}  [200]
   │   │
   │   ├─→ [POST] /medicines (Create Low Stock Medicine)
   │   │   Request:  {"name": "Paracetamol", "stock": 25, "expiryDate": "2025-12-31"}
   │   │   Response: {...}  [201]
   │   │
   │   ├─→ [GET] /medicines/low-stock?minimumStock=50 (List Low Stock)
   │   │   Response: [{...}, {...}]  [200]
   │   │
   │   ├─→ [PUT] /medicines/1 (Update Medicine)
   │   │   Request:  {"name": "Aspirin", "stock": 200, "expiryDate": "2025-12-31"}
   │   │   Response: {"medicine_id": 1, "stock": 200, ...}  [200]
   │   │
   │   ├─→ [DELETE] /medicines/1 (Delete Medicine)
   │   │   Response: (No Content)  [204]
   │   │
   │   └─→ [GET] /medicines/1 (Verify Deletion)
   │       Response: Error  [404]
   │
   ├─→ 6. APPOINTMENT MANAGEMENT
   │   ├─→ [POST] /appointments (Create Appointment)
   │   │   Request:  {"patientId": 1, "doctorId": 10, "slotTimestamp": "2026-03-15T10:00:00"}
   │   │   Response: {"appointment_id": 1, ...}  [201]
   │   │   → Auto-saves: appointment_id = 1
   │   │
   │   ├─→ [GET] /appointments/1 (Get Appointment)
   │   │   Response: {...}  [200]
   │   │
   │   ├─→ [GET] /appointments/patient/1 (Patient Appointments)
   │   │   Response: [{...}]  [200]
   │   │
   │   ├─→ [GET] /appointments/doctor/10 (Doctor Appointments)
   │   │   Response: [{...}]  [200]
   │   │
   │   ├─→ [GET] /appointments/check-slot?doctorId=10&slotTimestamp=2026-04-15T14:00:00
   │   │   Response: {"available": true, ...}  [200]
   │   │
   │   └─→ [GET] /appointments/check-slot?doctorId=10&slotTimestamp=2026-03-15T10:00:00
   │       Response: {"available": false, ...}  [200]
   │
   ├─→ 7. PRESCRIPTION MANAGEMENT
   │   ├─→ [POST] /prescriptions (Create Prescription)
   │   │   Request:  {"patientId": 1, "doctorId": 10, "prescriptionDate": "2026-03-15"}
   │   │   Response: {"prescription_id": 1, ...}  [201]
   │   │   → Auto-saves: prescription_id = 1
   │   │
   │   ├─→ [GET] /prescriptions/1 (Get Prescription)
   │   │   Response: {...}  [200]
   │   │
   │   └─→ [GET] /prescriptions/patient/1 (Patient Prescriptions)
   │       Response: [{...}]  [200]
   │
   └─→ END: All Tests Complete ✓

```

---

## Endpoint Map (Visual)

```
BASE URL: http://localhost:8080/api

┌─ HEALTH
│  └─ GET /health
│
├─ PATIENTS
│  ├─ POST   /patients                    → Create
│  └─ GET    /patients/{id}               → Retrieve
│
├─ MEDICINES
│  ├─ POST   /medicines                   → Create
│  ├─ GET    /medicines/{id}              → Get
│  ├─ GET    /medicines/search/by-name    → Search
│  ├─ GET    /medicines/low-stock         → Low Stock List
│  ├─ GET    /medicines/expiring-before   → Expiring List
│  ├─ PUT    /medicines/{id}              → Update
│  └─ DELETE /medicines/{id}              → Delete
│
├─ APPOINTMENTS
│  ├─ POST   /appointments                → Create
│  ├─ GET    /appointments/{id}           → Get
│  ├─ GET    /appointments/patient/{pId}  → Patient's Appts
│  ├─ GET    /appointments/doctor/{dId}   → Doctor's Appts
│  └─ GET    /appointments/check-slot     → Check Availability
│
└─ PRESCRIPTIONS
   ├─ POST   /prescriptions               → Create
   ├─ GET    /prescriptions/{id}          → Get
   └─ GET    /prescriptions/patient/{pId} → Patient's Prescriptions
```

---

## Data Flow Diagram

```
╔════════════════════════════════════════════════════════════════╗
║                     Testing Workflow                           ║
╚════════════════════════════════════════════════════════════════╝

Create Patient (ID: 1)
    │
    ├─→ Create Medicine (ID: 1)
    │       │
    │       └─→ Medicine added to inventory
    │           (Stock: 100, Expiry: 2025-12-31)
    │
    ├─→ Create Appointment
    │       │
    │       ├─→ Assign to Patient (ID: 1)
    │       ├─→ Assign to Doctor (ID: 10)
    │       └─→ Schedule for 2026-03-15 10:00
    │
    └─→ Create Prescription
            │
            ├─→ Link to Patient (ID: 1)
            ├─→ Link to Doctor (ID: 10)
            └─→ Set Date: 2026-03-15


Query Operations:
    ↓
[Get Patient Appointments] → Returns appointment(s)
[Get Patient Prescriptions] → Returns prescription(s)
[Search Medicines] → Returns medicine by name
[Check Slot Availability] → Returns availability status


Update Operations:
    ↓
[Update Medicine Stock] → Stock: 100 → 200


Delete Operations:
    ↓
[Delete Medicine] → Removes from inventory
[Verify 404] → Confirms deletion
```

---

## Request/Response Cycle

```
CLIENT (Postman)                    SERVER (Spring Boot)               DATABASE
    │                                    │                                 │
    │─ POST /patients ─────────────→    │                                 │
    │  {"name": "John",                 │─ Validate Data ─────────────→  │
    │   "contact": "+1-555"}            │                                │
    │                                   │← Create Record ─────────────   │
    │                                   │      (ID: 1)                    │
    │← {"id": 1, ...} ────────────────  │                                 │
    │    (201 Created)                  │                                 │
    │
    │─ GET /patients/1 ───────────────→ │                                 │
    │                                   │─ Query Database ────────────→  │
    │                                   │       (ID=1)                    │
    │                                   │← Retrieve Record (John) ──   │
    │← {"id": 1, "name": "John"} ────── │                                 │
    │    (200 OK)                       │                                 │
    │
    │─ DELETE /patients/1 ────────────→ │                                 │
    │                                   │─ Delete Record ────────────→  │
    │                                   │       (ID=1)                    │
    │                                   │← Deleted ──────────────────   │
    │← (Empty) ─────────────────────── │                                 │
    │  (204 No Content)                 │                                 │
```

---

## Validation Rules Reference

```
PATIENTS
├─ name
│  ├─ Required: Yes (cannot be empty/blank)
│  ├─ Max Length: 255 characters
│  └─ Trim: Whitespace trimmed before save
│
└─ contact
   ├─ Required: Yes
   ├─ Format: Valid phone number
   └─ Accepted Formats:
      ├─ +1-555-0100
      ├─ +91-9876543210
      ├─ 5550100
      ├─ +1 (555) 0100
      └─ +1.555.0100

MEDICINES
├─ name
│  ├─ Required: Yes
│  └─ Max Length: 255 characters
│
├─ stock
│  ├─ Required: Yes
│  ├─ Must be: > 0 (positive)
│  └─ Cannot be: negative or zero
│
└─ expiryDate
   ├─ Required: Yes
   └─ Cannot be: in the past

APPOINTMENTS
├─ patientId
│  ├─ Required: Yes
│  └─ Must exist: in database
│
├─ doctorId
│  ├─ Required: Yes
│  └─ Must be: valid ID
│
├─ slotTimestamp
│  ├─ Required: Yes
│  ├─ Cannot be: in the past
│  └─ Cannot be: double-booked (same doctor, same time)
│
└─ status
   └─ Auto-set to: "SCHEDULED"

PRESCRIPTIONS
├─ patientId
│  ├─ Required: Yes
│  └─ Must exist: in database
│
├─ doctorId
│  ├─ Required: Yes
│  └─ Must be: valid ID
│
└─ prescriptionDate
   └─ Required: Yes
```

---

## Error Response Examples

```
400 BAD REQUEST (Validation Error)
{
  "status": 400,
  "message": "Validation failed",
  "error": "BAD_REQUEST",
  "timestamp": "2026-02-18T14:47:12.129385054",
  "path": "/api/patients",
  "validationErrors": {
    "name": "Patient name is required and cannot be empty",
    "contact": "Contact must be a valid phone number (e.g., +1-555-0100)"
  }
}

404 NOT FOUND (Resource Missing)
{
  "status": 404,
  "message": "Patient not found with ID: 999",
  "error": "NOT_FOUND",
  "timestamp": "2026-02-18T14:47:25.701194639",
  "path": "/api/patients/999",
  "validationErrors": null
}

500 INTERNAL SERVER ERROR (Unexpected Error)
{
  "status": 500,
  "message": "An unexpected error occurred",
  "error": "INTERNAL_SERVER_ERROR",
  "timestamp": "2026-02-18T14:47:30.123456789",
  "path": "/api/patients"
}
```

---

## Quick Status Code Reference

```
✅ SUCCESS RESPONSES
  2xx
  ├─ 200 OK              → GET, PUT succeeded
  ├─ 201 Created         → POST created new resource
  └─ 204 No Content      → DELETE succeeded (no body)

❌ CLIENT ERRORS
  4xx
  ├─ 400 Bad Request     → Invalid data/validation failed
  │   └─ Check validationErrors field
  │
  ├─ 404 Not Found       → Resource doesn't exist
  │   └─ Check ID exists in database
  │
  └─ 409 Conflict        → Resource already exists/conflict
      └─ Check for duplicates

❌ SERVER ERRORS
  5xx
  ├─ 500 Server Error    → Code error/unexpected issue
  │   └─ Check application logs
  │
  └─ 503 Unavailable     → Database/service down
      └─ Check database connection
```

---

## Testing Matrix

```
✓ = Should pass
✗ = Should fail (expect error)

OPERATION                          │ EXPECTATION
─────────────────────────────────────────────────
Health Check                       │ ✓ 200 OK
Create Patient (valid)             │ ✓ 201 Created
Create Patient (empty name)        │ ✗ 400 Bad Request
Create Patient (invalid contact)   │ ✗ 400 Bad Request
Get Patient (valid ID)             │ ✓ 200 OK
Get Patient (invalid ID)           │ ✗ 404 Not Found
Create Medicine (valid)            │ ✓ 201 Created
Create Medicine (negative stock)   │ ✗ 400 Bad Request
Create Medicine (past expiry)      │ ✗ 400 Bad Request
Get Medicine (valid ID)            │ ✓ 200 OK
Search Medicine (exists)           │ ✓ 200 OK (results)
Search Medicine (not exists)       │ ✓ 200 OK (empty)
Update Medicine                    │ ✓ 200 OK
Delete Medicine                    │ ✓ 204 No Content
Get Deleted Medicine               │ ✗ 404 Not Found
List Low Stock (threshold: 50)     │ ✓ 200 OK (filtered)
Create Appointment (future)        │ ✓ 201 Created
Create Appointment (past)          │ ✗ 400 Bad Request
Check Slot (available)             │ ✓ 200 OK (true)
Check Slot (occupied)              │ ✓ 200 OK (false)
Get Patient Appointments           │ ✓ 200 OK (array)
Create Prescription                │ ✓ 201 Created
Get Prescription                   │ ✓ 200 OK
```

---

## Time Estimates

```
Activity                          │ Time
──────────────────────────────────────────
Setup (import + environment)      │ 2 min
Health Check                      │ 1 min
Patient Tests                     │ 3 min
Medicine Tests                    │ 5 min
Appointment Tests                 │ 4 min
Prescription Tests                │ 2 min
Validation Error Tests            │ 3 min
─────────────────────────────────────────
TOTAL (Full Coverage)             │ ~20 min
```

---

## Next Steps

1. ✓ Read this file (Visual understanding)
2. → Read POSTMAN_QUICK_START.md (Implementation)
3. → Read POSTMAN_TESTING_GUIDE.md (Detailed steps)
4. → Import medi-d-postman-collection.json (Actual testing)
5. → Run tests in Postman (Verification)

Happy Testing! 🚀
