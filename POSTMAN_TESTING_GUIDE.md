# Postman Testing Guide for Medi-D

## Table of Contents
1. [Setup & Prerequisites](#setup--prerequisites)
2. [Project Overview](#project-overview)
3. [All Available Endpoints](#all-available-endpoints)
4. [Step-by-Step Testing Guide](#step-by-step-testing-guide)
5. [Test Scenarios & Validation](#test-scenarios--validation)
6. [Troubleshooting](#troubleshooting)

---

## Setup & Prerequisites

### 1. **Install Postman**
- Download from: [https://www.postman.com/downloads/](https://www.postman.com/downloads/)
- Install and launch Postman

### 2. **Start Your Application**
```bash
# From your project root directory
chmod +x ./mvnw
./mvnw spring-boot:run
```

Or use your run script:
```bash
./run-app.sh
```

The application will run on: **http://localhost:8080**

### 3. **Ensure Database is Running**
```bash
# Check database connection
./test-db-connection.sh

# Or setup database if needed
./setup-db.sh
```

### 4. **Import the Postman Collection**
- File menu → Import
- Choose the provided `medi-d-postman-collection.json`
- Collection will be imported with all endpoints

---

## Project Overview

**Medi-D** is a Healthcare Management System with the following modules:

| Module | Purpose | Endpoints |
|--------|---------|-----------|
| **Patient Management** | Manage patient records | Create, Get |
| **Medicine Management** | Manage pharmacy inventory | Create, Get, Update, Delete, Search |
| **Appointment Management** | Schedule and manage appointments | Create, Get, List, Check slots |
| **Prescription Management** | Manage prescriptions | Create, Get, Update, List |

---

## All Available Endpoints

### Base URL
```
http://localhost:8080/api
```

### 1. Health Check
```
GET /api/health
Response: 200 OK
Body: "Medi-D System is up and running!"
```

---

### 2. Patient Management

#### Create Patient
```
POST /api/patients
Content-Type: application/json

Request Body:
{
  "name": "John Doe",
  "contact": "+1-555-0100"
}

Response: 201 Created
{
  "id": 1,
  "name": "John Doe"
}
```

#### Get Patient
```
GET /api/patients/{id}

Response: 200 OK
{
  "id": 1,
  "name": "John Doe"
}
```

---

### 3. Medicine Management

#### Create Medicine
```
POST /api/medicines
Content-Type: application/json

Request Body:
{
  "name": "Aspirin",
  "stock": 100,
  "expiryDate": "2025-12-31"
}

Response: 201 Created
{
  "medicine_id": 1,
  "medicine_name": "Aspirin",
  "stock": 100,
  "expiry_date": "2025-12-31",
  "version": 1
}
```

#### Get Medicine
```
GET /api/medicines/{id}

Response: 200 OK
{
  "medicine_id": 1,
  "medicine_name": "Aspirin",
  "stock": 100,
  "expiry_date": "2025-12-31",
  "version": 1
}
```

#### Search Medicine by Name
```
GET /api/medicines/search/by-name?name=Aspirin

Response: 200 OK
{
  "medicine_id": 1,
  "medicine_name": "Aspirin",
  "stock": 100,
  "expiry_date": "2025-12-31",
  "version": 1
}
```

#### List Low Stock Medicines
```
GET /api/medicines/low-stock?minimumStock=50

Response: 200 OK
[
  {
    "medicine_id": 1,
    "medicine_name": "Aspirin",
    "stock": 25,
    "expiry_date": "2025-12-31",
    "version": 1
  }
]
```

#### List Expiring Medicines
```
GET /api/medicines/expiring-before?expiryDate=2024-06-30

Response: 200 OK
[
  {
    "medicine_id": 1,
    "medicine_name": "Aspirin",
    "stock": 100,
    "expiry_date": "2024-05-31",
    "version": 1
  }
]
```

#### Update Medicine
```
PUT /api/medicines/{id}
Content-Type: application/json

Request Body:
{
  "name": "Aspirin",
  "stock": 150,
  "expiryDate": "2025-12-31"
}

Response: 200 OK
{
  "medicine_id": 1,
  "medicine_name": "Aspirin",
  "stock": 150,
  "expiry_date": "2025-12-31",
  "version": 2
}
```

#### Delete Medicine
```
DELETE /api/medicines/{id}

Response: 204 No Content
```

---

### 4. Appointment Management

#### Create Appointment
```
POST /api/appointments
Content-Type: application/json

Request Body:
{
  "patientId": 1,
  "doctorId": 10,
  "slotTimestamp": "2024-03-15T10:00:00"
}

Response: 201 Created
{
  "appointment_id": 1,
  "patient_id": 1,
  "patient_name": "John Doe",
  "doctor_id": 10,
  "slot_timestamp": "2024-03-15T10:00:00",
  "status": "SCHEDULED"
}
```

#### Get Appointment
```
GET /api/appointments/{id}

Response: 200 OK
{
  "appointment_id": 1,
  "patient_id": 1,
  "patient_name": "John Doe",
  "doctor_id": 10,
  "slot_timestamp": "2024-03-15T10:00:00",
  "status": "SCHEDULED"
}
```

#### Get Patient Appointments
```
GET /api/appointments/patient/{patientId}

Response: 200 OK
[
  {
    "appointment_id": 1,
    "patient_id": 1,
    ...
  }
]
```

#### Get Doctor Appointments
```
GET /api/appointments/doctor/{doctorId}

Response: 200 OK
[
  {
    "appointment_id": 1,
    ...
  }
]
```

#### Check Slot Availability
```
GET /api/appointments/check-slot?doctorId=10&slotTimestamp=2024-03-15T10:00:00

Response: 200 OK
{
  "available": true,
  "doctorId": 10,
  "slotTimestamp": "2024-03-15T10:00:00"
}
```

---

### 5. Prescription Management

#### Create Prescription
```
POST /api/prescriptions
Content-Type: application/json

Request Body:
{
  "patientId": 1,
  "doctorId": 10,
  "prescriptionDate": "2024-03-15"
}

Response: 201 Created
{
  "prescription_id": 1,
  "patient_id": 1,
  "doctor_id": 10,
  "prescription_date": "2024-03-15",
  "status": "ACTIVE"
}
```

#### Get Prescription
```
GET /api/prescriptions/{id}

Response: 200 OK
{
  "prescription_id": 1,
  "patient_id": 1,
  "doctor_id": 10,
  "prescription_date": "2024-03-15",
  "status": "ACTIVE"
}
```

#### Get Patient Prescriptions
```
GET /api/prescriptions/patient/{patientId}

Response: 200 OK
[
  {
    "prescription_id": 1,
    ...
  }
]
```

---

## Step-by-Step Testing Guide

### Phase 1: Basic Setup & Health Check

#### Step 1: Test Health Check
1. Open Postman
2. Create a new tab
3. **Method:** GET
4. **URL:** `http://localhost:8080/api/health`
5. Click **Send**
6. **Expected Response:** 200 OK with message "Medi-D System is up and running!"

---

### Phase 2: Patient Management Testing

#### Step 2: Create a Patient
1. **Method:** POST
2. **URL:** `http://localhost:8080/api/patients`
3. **Headers:** 
   - Key: `Content-Type`
   - Value: `application/json`
4. **Body (raw JSON):**
```json
{
  "name": "John Doe",
  "contact": "+1-555-0100"
}
```
5. Click **Send**
6. **Expected Response:** 201 Created
7. **Note the returned patient ID** (e.g., 1) - you'll need it for other tests

#### Step 3: Retrieve the Patient
1. **Method:** GET
2. **URL:** `http://localhost:8080/api/patients/1` (replace 1 with your patient ID)
3. Click **Send**
4. **Expected Response:** 200 OK with patient details

#### Step 4: Test Invalid Patient ID
1. **Method:** GET
2. **URL:** `http://localhost:8080/api/patients/999`
3. Click **Send**
4. **Expected Response:** 404 Not Found

---

### Phase 3: Medicine Management Testing

#### Step 5: Create a Medicine
1. **Method:** POST
2. **URL:** `http://localhost:8080/api/medicines`
3. **Headers:** `Content-Type: application/json`
4. **Body:**
```json
{
  "name": "Aspirin",
  "stock": 100,
  "expiryDate": "2025-12-31"
}
```
5. Click **Send**
6. **Expected Response:** 201 Created
7. **Note the medicine ID**

#### Step 6: Get Medicine
1. **Method:** GET
2. **URL:** `http://localhost:8080/api/medicines/1`
3. Click **Send**
4. **Expected Response:** 200 OK

#### Step 7: Search Medicine by Name
1. **Method:** GET
2. **URL:** `http://localhost:8080/api/medicines/search/by-name?name=Aspirin`
3. Click **Send**
4. **Expected Response:** 200 OK with matching medicine

#### Step 8: Create Low Stock Medicine
1. **Method:** POST
2. **URL:** `http://localhost:8080/api/medicines`
3. **Body:**
```json
{
  "name": "Paracetamol",
  "stock": 25,
  "expiryDate": "2025-12-31"
}
```
5. Click **Send**
6. **Expected Response:** 201 Created

#### Step 9: List Low Stock Medicines
1. **Method:** GET
2. **URL:** `http://localhost:8080/api/medicines/low-stock?minimumStock=50`
3. Click **Send**
4. **Expected Response:** 200 OK with Paracetamol in the list

#### Step 10: Update Medicine
1. **Method:** PUT
2. **URL:** `http://localhost:8080/api/medicines/1`
3. **Headers:** `Content-Type: application/json`
4. **Body:**
```json
{
  "name": "Aspirin",
  "stock": 200,
  "expiryDate": "2025-12-31"
}
```
5. Click **Send**
6. **Expected Response:** 200 OK with updated stock

#### Step 11: Delete Medicine
1. **Method:** DELETE
2. **URL:** `http://localhost:8080/api/medicines/1`
3. Click **Send**
4. **Expected Response:** 204 No Content

#### Step 12: Verify Deletion
1. **Method:** GET
2. **URL:** `http://localhost:8080/api/medicines/1`
3. Click **Send**
4. **Expected Response:** 404 Not Found

---

### Phase 4: Appointment Management Testing

#### Step 13: Create Appointment
1. **Method:** POST
2. **URL:** `http://localhost:8080/api/appointments`
3. **Headers:** `Content-Type: application/json`
4. **Body:**
```json
{
  "patientId": 1,
  "doctorId": 10,
  "slotTimestamp": "2026-03-15T10:00:00"
}
```
5. Click **Send**
6. **Expected Response:** 201 Created
7. **Note the appointment ID**

#### Step 14: Get Appointment
1. **Method:** GET
2. **URL:** `http://localhost:8080/api/appointments/1`
3. Click **Send**
4. **Expected Response:** 200 OK

#### Step 15: Get Patient Appointments
1. **Method:** GET
2. **URL:** `http://localhost:8080/api/appointments/patient/1`
3. Click **Send**
4. **Expected Response:** 200 OK with list of appointments

#### Step 16: Get Doctor Appointments
1. **Method:** GET
2. **URL:** `http://localhost:8080/api/appointments/doctor/10`
3. Click **Send**
4. **Expected Response:** 200 OK

#### Step 17: Check Slot Availability (Available)
1. **Method:** GET
2. **URL:** `http://localhost:8080/api/appointments/check-slot?doctorId=10&slotTimestamp=2026-04-15T14:00:00`
3. Click **Send**
4. **Expected Response:** 200 OK with `"available": true`

#### Step 18: Check Slot Availability (Occupied)
1. **Method:** GET
2. **URL:** `http://localhost:8080/api/appointments/check-slot?doctorId=10&slotTimestamp=2026-03-15T10:00:00`
3. Click **Send**
4. **Expected Response:** 200 OK with `"available": false`

---

### Phase 5: Prescription Management Testing

#### Step 19: Create Prescription
1. **Method:** POST
2. **URL:** `http://localhost:8080/api/prescriptions`
3. **Headers:** `Content-Type: application/json`
4. **Body:**
```json
{
  "patientId": 1,
  "doctorId": 10,
  "prescriptionDate": "2026-03-15"
}
```
5. Click **Send**
6. **Expected Response:** 201 Created

#### Step 20: Get Prescription
1. **Method:** GET
2. **URL:** `http://localhost:8080/api/prescriptions/1`
3. Click **Send**
4. **Expected Response:** 200 OK

#### Step 21: Get Patient Prescriptions
1. **Method:** GET
2. **URL:** `http://localhost:8080/api/prescriptions/patient/1`
3. Click **Send**
4. **Expected Response:** 200 OK with list of prescriptions

---

## Test Scenarios & Validation

### Validation Tests

#### 1. Patient Name Validation
**Test:** Create patient with empty name
```json
{
  "name": "",
  "contact": "+1-555-0100"
}
```
**Expected Response:** 400 Bad Request with validation error

**Test:** Create patient with valid name
```json
{
  "name": "Jane Smith",
  "contact": "+91-9876543210"
}
```
**Expected Response:** 201 Created

#### 2. Contact Format Validation
**Test:** Create patient with invalid phone format
```json
{
  "name": "John Doe",
  "contact": "invalid"
}
```
**Expected Response:** 400 Bad Request with validation error

**Valid formats:**
- `+1-555-0100`
- `+91-9876543210`
- `5550100`
- `+1 (555) 0100`
- `+1.555.0100`

#### 3. Medicine Stock Validation
**Test:** Create medicine with negative stock
```json
{
  "name": "Aspirin",
  "stock": -10,
  "expiryDate": "2025-12-31"
}
```
**Expected Response:** 400 Bad Request

**Test:** Create medicine with past expiry
```json
{
  "name": "Aspirin",
  "stock": 100,
  "expiryDate": "2020-12-31"
}
```
**Expected Response:** 400 Bad Request

#### 4. Appointment Validation
**Test:** Create appointment in the past
```json
{
  "patientId": 1,
  "doctorId": 10,
  "slotTimestamp": "2020-03-15T10:00:00"
}
```
**Expected Response:** 400 Bad Request

---

### Integration Tests

#### Test Complete Patient-Medicine-Appointment Workflow
1. Create a patient
2. Create 2 medicines
3. Create an appointment for the patient
4. Create a prescription for the patient
5. Get all patient appointments
6. Get all patient prescriptions
7. Update medicine stock
8. List low stock medicines

---

## Using Variables/Environment in Postman

### Create Environment
1. Click **Environments** (left panel)
2. Click **Create**
3. Add variables:

| Variable Name | Initial Value |
|--------------|--------------|
| base_url | http://localhost:8080/api |
| patient_id | 1 |
| medicine_id | 1 |
| appointment_id | 1 |
| doctor_id | 10 |

### Use Variables in Requests
Instead of `http://localhost:8080/api/patients/1`, use:
```
{{base_url}}/patients/{{patient_id}}
```

---

## Request Response Examples

### Success Response Format (2xx)
```json
{
  "id": 1,
  "name": "John Doe"
}
```

### Error Response Format (4xx, 5xx)
```json
{
  "status": 400,
  "message": "Validation failed",
  "error": "BAD_REQUEST",
  "timestamp": "2026-02-18T14:47:12.129385054",
  "path": "/api/patients",
  "validationErrors": {
    "name": "Patient name is required and cannot be empty",
    "contact": "Contact must be a valid phone number"
  }
}
```

---

## Troubleshooting

### Problem: "Connection refused" or "Cannot reach server"
**Solution:**
1. Ensure application is running: `./mvnw spring-boot:run`
2. Check port 8080 is accessible
3. Verify URL in Postman: `http://localhost:8080/api`

### Problem: "404 Not Found" on health check
**Solution:**
1. Verify endpoint: Should be `/api/health`, not `/health`
2. Check base URL spelling

### Problem: "400 Bad Request" with validation errors
**Solution:**
1. Check JSON format is valid (use JSON formatter)
2. Verify required fields are present
3. Check data types (string, number, date format)

### Problem: "204 No Content" when expecting response
**Solution:**
- This is correct behavior for DELETE and UPDATE operations
- It means the operation was successful

### Problem: "401 Unauthorized"
**Solution:**
- Currently, authentication is not enforced
- All endpoints are public
- If this appears, check if feature was recently added

---

## Performance Testing Tips

### Test Load/Stress
1. Use Postman's **Runner** feature
2. Create a test collection
3. Run multiple requests in sequence
4. Monitor response times

### Monitor Database
```bash
psql -h localhost -p 5432 -U medid_admin -d medid_test
```

---

## Next Steps

- Read [DEVELOPMENT_GUIDE.md](DEVELOPMENT_GUIDE.md) for architecture details
- Check [TEST_RESULTS_REPORT.md](TEST_RESULTS_REPORT.md) for unit test references
- Review database schema in [DB_SETUP_GUIDE.md](DB_SETUP_GUIDE.md)

---

## Quick Reference Table

| Operation | Method | URL | Status |
|-----------|--------|-----|--------|
| Health Check | GET | `/health` | 200 |
| Create Patient | POST | `/patients` | 201 |
| Get Patient | GET | `/patients/{id}` | 200 |
| Create Medicine | POST | `/medicines` | 201 |
| Get Medicine | GET | `/medicines/{id}` | 200 |
| Search Medicine | GET | `/medicines/search/by-name?name=X` | 200 |
| Low Stock | GET | `/medicines/low-stock?minimumStock=X` | 200 |
| Expiring | GET | `/medicines/expiring-before?expiryDate=X` | 200 |
| Update Medicine | PUT | `/medicines/{id}` | 200 |
| Delete Medicine | DELETE | `/medicines/{id}` | 204 |
| Create Appointment | POST | `/appointments` | 201 |
| Get Appointment | GET | `/appointments/{id}` | 200 |
| Patient Appointments | GET | `/appointments/patient/{patientId}` | 200 |
| Doctor Appointments | GET | `/appointments/doctor/{doctorId}` | 200 |
| Check Slot | GET | `/appointments/check-slot?doctorId=X&slotTimestamp=X` | 200 |
| Create Prescription | POST | `/prescriptions` | 201 |
| Get Prescription | GET | `/prescriptions/{id}` | 200 |
| Patient Prescriptions | GET | `/prescriptions/patient/{patientId}` | 200 |
