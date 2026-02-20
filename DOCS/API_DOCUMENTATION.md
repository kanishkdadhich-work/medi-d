# Medi-D REST API Documentation

## Base URL

```
http://localhost:8080/api
```

---

## Authentication

Currently, all endpoints are public. For production, implement Spring Security with:
- JWT tokens
- Role-based access control (ADMIN, DOCTOR, PATIENT)
- Token refresh mechanism

---

## Response Format

### Success Response (2xx)

```json
{
  "data": {
    "id": 1,
    "name": "Sample",
    ...
  },
  "message": "Operation successful",
  "timestamp": "2024-02-18T10:30:00Z"
}
```

### Error Response (4xx, 5xx)

```json
{
  "error": "Resource not found",
  "message": "Patient with ID 999 not found",
  "status": 404,
  "timestamp": "2024-02-18T10:30:00Z"
}
```

---

## Patient Management

### Get Patient

```http
GET /api/patients/{id}
```

**Parameters:**
- `id` (path, required) - Patient ID

**Response (200):**
```json
{
  "patient_id": 1,
  "patient_name": "John Doe",
  "phone_number": "9876543210",
  "email": "john@example.com",
  "address": "123 Main St"
}
```

**Errors:**
- `404` - Patient not found
- `400` - Invalid patient ID

---

### Create Patient

```http
POST /api/patients
Content-Type: application/json
```

**Request Body:**
```json
{
  "name": "John Doe",
  "phone_number": "9876543210",
  "email": "john@example.com",
  "address": "123 Main St"
}
```

**Response (201):**
```json
{
  "patient_id": 1,
  "patient_name": "John Doe",
  "phone_number": "9876543210",
  "email": "john@example.com",
  "address": "123 Main St"
}
```

**Validation Errors (400):**
- Name required and not blank
- Phone number required
- Valid email format required

---

## Medicine Management

### Get Medicine

```http
GET /api/medicines/{id}
```

**Response (200):**
```json
{
  "medicine_id": 1,
  "medicine_name": "Aspirin",
  "stock": 100,
  "expiry_date": "2025-12-31",
  "version": 1
}
```

---

### Search Medicine by Name

```http
GET /api/medicines/search/by-name?name=Aspirin
```

**Query Parameters:**
- `name` (required) - Medicine name

**Response (200):**
```json
{
  "medicine_id": 1,
  "medicine_name": "Aspirin",
  "stock": 100,
  "expiry_date": "2025-12-31",
  "version": 1
}
```

---

### List Expiring Medicines

```http
GET /api/medicines/expiring-before?expiryDate=2024-06-30
```

**Query Parameters:**
- `expiryDate` (required) - Date in format YYYY-MM-DD

**Response (200):**
```json
[
  {
    "medicine_id": 1,
    "medicine_name": "Aspirin",
    "stock": 10,
    "expiry_date": "2024-05-31",
    "version": 1
  }
]
```

---

### List Low Stock Medicines

```http
GET /api/medicines/low-stock?minimumStock=50
```

**Query Parameters:**
- `minimumStock` (required) - Minimum stock threshold

**Response (200):**
```json
[
  {
    "medicine_id": 2,
    "medicine_name": "Paracetamol",
    "stock": 25,
    "expiry_date": "2025-12-31",
    "version": 1
  }
]
```

---

### Create Medicine

```http
POST /api/medicines
Content-Type: application/json
```

**Request Body:**
```json
{
  "name": "Aspirin",
  "stock": 100,
  "expiryDate": "2025-12-31"
}
```

**Response (201):**
```json
{
  "medicine_id": 1,
  "medicine_name": "Aspirin",
  "stock": 100,
  "expiry_date": "2025-12-31",
  "version": 1
}
```

**Validation Errors (400):**
- Name required and not blank
- Stock must be positive
- Expiry date must not be in the past

---

### Update Medicine

```http
PUT /api/medicines/{id}
Content-Type: application/json
```

**Request Body:**
```json
{
  "name": "Aspirin",
  "stock": 150,
  "expiryDate": "2025-12-31"
}
```

**Response (200):**
```json
{
  "medicine_id": 1,
  "medicine_name": "Aspirin",
  "stock": 150,
  "expiry_date": "2025-12-31",
  "version": 2
}
```

---

### Delete Medicine

```http
DELETE /api/medicines/{id}
```

**Response (204):** No Content

---

## Appointment Management

### Get Appointment

```http
GET /api/appointments/{id}
```

**Response (200):**
```json
{
  "appointment_id": 1,
  "patient_id": 1,
  "patient_name": "John Doe",
  "doctor_id": 10,
  "slot_timestamp": "2024-03-15T10:00:00",
  "status": "SCHEDULED"
}
```

---

### Get Patient Appointments

```http
GET /api/appointments/patient/{patientId}
```

**Response (200):**
```json
[
  {
    "appointment_id": 1,
    "patient_id": 1,
    "patient_name": "John Doe",
    "doctor_id": 10,
    "slot_timestamp": "2024-03-15T10:00:00",
    "status": "SCHEDULED"
  }
]
```

---

### Get Doctor Appointments

```http
GET /api/appointments/doctor/{doctorId}
```

**Response (200):**
```json
[
  {
    "appointment_id": 1,
    "patient_id": 1,
    "patient_name": "John Doe",
    "doctor_id": 10,
    "slot_timestamp": "2024-03-15T10:00:00",
    "status": "SCHEDULED"
  }
]
```

---

### Check Slot Availability

```http
GET /api/appointments/check-slot?doctorId=10&slotTimestamp=2024-03-15T10:00:00
```

**Query Parameters:**
- `doctorId` (required) - Doctor ID
- `slotTimestamp` (required) - Slot time in ISO format

**Response (200):**
```json
{
  "available": true,
  "doctor_id": 10,
  "slot_timestamp": "2024-03-15T10:00:00"
}
```

---

### Create Appointment

```http
POST /api/appointments
Content-Type: application/json
```

**Request Body:**
```json
{
  "patient_id": 1,
  "doctor_id": 10,
  "slot_timestamp": "2024-03-15T10:00:00",
  "status": "SCHEDULED"
}
```

**Response (201):**
```json
{
  "appointment_id": 1,
  "patient_id": 1,
  "patient_name": "John Doe",
  "doctor_id": 10,
  "slot_timestamp": "2024-03-15T10:00:00",
  "status": "SCHEDULED"
}
```

**Validation Errors (400):**
- Patient ID must be positive
- Doctor ID must be positive
- Slot timestamp must be in the future
- Status is required

---

### Update Appointment

```http
PUT /api/appointments/{id}
Content-Type: application/json
```

**Request Body:**
```json
{
  "patient_id": 1,
  "doctor_id": 10,
  "slot_timestamp": "2024-03-15T11:00:00",
  "status": "RESCHEDULED"
}
```

**Response (200):**
```json
{
  "appointment_id": 1,
  "patient_id": 1,
  "patient_name": "John Doe",
  "doctor_id": 10,
  "slot_timestamp": "2024-03-15T11:00:00",
  "status": "RESCHEDULED"
}
```

---

### Filter Appointments by Status

```http
GET /api/appointments/by-status/SCHEDULED
```

**Response (200):**
```json
[
  {
    "appointment_id": 1,
    "patient_id": 1,
    "patient_name": "John Doe",
    "doctor_id": 10,
    "slot_timestamp": "2024-03-15T10:00:00",
    "status": "SCHEDULED"
  }
]
```

---

### Delete Appointment

```http
DELETE /api/appointments/{id}
```

**Response (204):** No Content

---

## Prescription Management

### Get Prescription

```http
GET /api/prescriptions/{id}
```

**Response (200):**
```json
{
  "prescription_id": 1,
  "appointment_id": 1,
  "diagnosis": "Common cold",
  "status": "PENDING",
  "created_at": "2024-02-18T10:30:00"
}
```

---

### Get Prescription by Appointment

```http
GET /api/prescriptions/appointment/{appointmentId}
```

**Response (200):**
```json
{
  "prescription_id": 1,
  "appointment_id": 1,
  "diagnosis": "Common cold",
  "status": "PENDING",
  "created_at": "2024-02-18T10:30:00"
}
```

---

### Get Pending Prescriptions

```http
GET /api/prescriptions/pending
```

**Response (200):**
```json
[
  {
    "prescription_id": 1,
    "appointment_id": 1,
    "diagnosis": "Common cold",
    "status": "PENDING",
    "created_at": "2024-02-18T10:30:00"
  }
]
```

---

### Get Dispensed Prescriptions

```http
GET /api/prescriptions/dispensed
```

**Response (200):**
```json
[
  {
    "prescription_id": 2,
    "appointment_id": 2,
    "diagnosis": "Headache",
    "status": "DISPENSED",
    "created_at": "2024-02-17T10:30:00"
  }
]
```

---

### Get Patient Prescriptions

```http
GET /api/prescriptions/patient/{patientId}
```

**Response (200):**
```json
[
  {
    "prescription_id": 1,
    "appointment_id": 1,
    "diagnosis": "Common cold",
    "status": "PENDING",
    "created_at": "2024-02-18T10:30:00"
  }
]
```

---

### Get Pending Prescription Count

```http
GET /api/prescriptions/patient/{patientId}/pending-count
```

**Response (200):**
```json
{
  "patient_id": 1,
  "pending_count": 2
}
```

---

### Create Prescription

```http
POST /api/prescriptions
Content-Type: application/json
```

**Request Body:**
```json
{
  "appointment_id": 1,
  "diagnosis": "Common cold",
  "status": "PENDING"
}
```

**Response (201):**
```json
{
  "prescription_id": 1,
  "appointment_id": 1,
  "diagnosis": "Common cold",
  "status": "PENDING",
  "created_at": "2024-02-18T10:30:00"
}
```

**Validation Errors (400):**
- Appointment ID must be positive
- Diagnosis required and not blank
- Status is required

---

### Update Prescription

```http
PUT /api/prescriptions/{id}
Content-Type: application/json
```

**Request Body:**
```json
{
  "appointment_id": 1,
  "diagnosis": "Cold with cough",
  "status": "DISPENSED"
}
```

**Response (200):**
```json
{
  "prescription_id": 1,
  "appointment_id": 1,
  "diagnosis": "Cold with cough",
  "status": "DISPENSED",
  "created_at": "2024-02-18T10:30:00"
}
```

---

### Delete Prescription

```http
DELETE /api/prescriptions/{id}
```

**Response (204):** No Content

---

## Prescription Item Management

### Get Prescription Item

```http
GET /api/prescription-items/{id}
```

**Response (200):**
```json
{
  "item_id": 1,
  "prescription_id": 1,
  "medicine_id": 1,
  "medicine_name": "Aspirin",
  "quantity_required": 10
}
```

---

### Get Items by Prescription

```http
GET /api/prescription-items/prescription/{prescriptionId}
```

**Response (200):**
```json
[
  {
    "item_id": 1,
    "prescription_id": 1,
    "medicine_id": 1,
    "medicine_name": "Aspirin",
    "quantity_required": 10
  }
]
```

---

### Get Items by Medicine

```http
GET /api/prescription-items/medicine/{medicineId}
```

**Response (200):**
```json
[
  {
    "item_id": 1,
    "prescription_id": 1,
    "medicine_id": 1,
    "medicine_name": "Aspirin",
    "quantity_required": 10
  }
]
```

---

### Get Pending Items for Medicine

```http
GET /api/prescription-items/medicine/{medicineId}/pending
```

**Response (200):**
```json
[
  {
    "item_id": 1,
    "prescription_id": 1,
    "medicine_id": 1,
    "medicine_name": "Aspirin",
    "quantity_required": 10
  }
]
```

---

### Create Prescription Item

```http
POST /api/prescription-items
Content-Type: application/json
```

**Request Body:**
```json
{
  "prescription_id": 1,
  "medicine_id": 1,
  "quantity_required": 10
}
```

**Response (201):**
```json
{
  "item_id": 1,
  "prescription_id": 1,
  "medicine_id": 1,
  "medicine_name": "Aspirin",
  "quantity_required": 10
}
```

**Validation Errors (400):**
- Prescription ID must be positive
- Medicine ID must be positive
- Quantity required must be positive

---

### Update Prescription Item

```http
PUT /api/prescription-items/{id}
Content-Type: application/json
```

**Request Body:**
```json
{
  "prescription_id": 1,
  "medicine_id": 1,
  "quantity_required": 15
}
```

**Response (200):**
```json
{
  "item_id": 1,
  "prescription_id": 1,
  "medicine_id": 1,
  "medicine_name": "Aspirin",
  "quantity_required": 15
}
```

---

### Delete Prescription Item

```http
DELETE /api/prescription-items/{id}
```

**Response (204):** No Content

---

## Generic Error Codes

| Code | Message | Cause |
|------|---------|-------|
| 400 | Bad Request | Invalid input or validation error |
| 404 | Not Found | Resource doesn't exist |
| 500 | Internal Server Error | Server-side error |

---

## Example cURL Commands

### Get Patient
```bash
curl -X GET http://localhost:8080/api/patients/1
```

### Create Medicine
```bash
curl -X POST http://localhost:8080/api/medicines \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Aspirin",
    "stock": 100,
    "expiryDate": "2025-12-31"
  }'
```

### Get Pending Prescriptions
```bash
curl -X GET http://localhost:8080/api/prescriptions/pending
```

### Check Slot Availability
```bash
curl -X GET 'http://localhost:8080/api/appointments/check-slot?doctorId=10&slotTimestamp=2024-03-15T10:00:00'
```

---

## Rate Limiting

Currently not implemented. Future enhancement:
- 100 requests per minute per IP
- 1000 requests per hour per user
- Exponential backoff on limit breach

---

## Versioning

API version is embedded in endpoint URLs: `/api/v1/...` (future enhancement)

---

## Pagination

Future enhancement for list endpoints:
```
GET /api/medicines?page=0&size=20&sort=name,asc
```

---

## Filtering & Sorting

Future enhancement for complex queries:
```
GET /api/prescriptions?status=PENDING&sort=created_at,desc
```

---

## Security Headers

Future production implementation:
- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY`
- `X-XSS-Protection: 1; mode=block`
- `Strict-Transport-Security: max-age=31536000`

