# Medi-D Database Schema - Clinic Management Module

## 📊 Entity Relationship Diagram

```
                            ┌─────────────┐
                            │   Patient   │
                            └──────┬──────┘
                                   │
                                   │ 1..* (One Patient has Many)
                                   │
                            ┌──────▼──────────┐
                            │  Appointment    │
                            │   (1:1 ref)     │
                            └──────┬──────────┘
                                   │
                                   │ 1..1 (One Appointment has One)
                                   │
                            ┌──────▼──────────┐
                            │  Prescription   │
                            │  (1:* items)    │
                            └──────┬──────────┘
                                   │
                                   │ 1..* (One Prescription has Many Items)
                                   │
                    ┌──────────────┴──────────────┐
                    │                             │
            ┌───────▼──────────┐        ┌────────▼─────┐
            │ PrescriptionItem │        │   Medicine   │
            │  (Many:Many)     │        │  (w/ Version)│
            └──────────────────┘        └──────────────┘
```

---

## 🗂️ Entity Classes Overview

### 1. **Patient** (Existing)
- **Purpose:** Store patient information
- **Relationships:** 1 → Many Appointments

### 2. **Medicine** ⭐ NEW
- **Purpose:** Store medicine inventory and stock information
- **Critical Feature:** `@Version` for optimistic locking
- **Key Fields:**
  - `id` - Auto-generated primary key
  - `name` - Medicine name
  - `stock` - Current stock count
  - `expiryDate` - Expiration date
  - `version` - Optimistic locking (prevents race conditions)
  - `createdAt` / `updatedAt` - Timestamps

**Why Version is Critical:**
```
Multiple concurrent requests trying to update medicine stock:
Request 1: Buy 5 units (stock was 10)
Request 2: Buy 3 units (stock was 10)

Without @Version:
Both requests read stock=10, decrement independently
Result: stock=7 (WRONG! Should be 2)

With @Version (Optimistic Locking):
Request 1 updates: stock=5, version increments (0→1)
Request 2 fails: version mismatch (0 != 1)
Result: Retry with fresh stock value
Final: stock=2 (CORRECT!)
```

**Indexes:**
- `idx_medicine_name` - For name searches
- `idx_medicine_expiry` - For finding expiring medicines

---

### 3. **Appointment** ⭐ NEW
- **Purpose:** Store appointment bookings between patients and doctors
- **Relationships:**
  - Many-to-One with Patient (via `@ManyToOne`)
  - One-to-One with Prescription
- **Key Fields:**
  - `id` - Auto-generated primary key
  - `doctorId` - Doctor identifier (Long, for future user integration)
  - `patient` - Reference to Patient entity (with EAGER fetch)
  - `slotTimestamp` - Appointment date/time
  - `status` - "BOOKED", "COMPLETED", "CANCELLED"
  - `createdAt` / `updatedAt` - Timestamps

**Cascade Behavior:**
```
PERSIST, MERGE - When saving/updating appointment, cascade to patient ops
Orphan Removal: NO - Patient can exist without appointments
```

**Indexes:**
```
idx_appointment_doctor - Query by doctor
idx_appointment_patient - Query by patient
idx_appointment_slot - Query by time range
idx_appointment_status - Query by status
```

---

### 4. **Prescription** ⭐ NEW
- **Purpose:** Store prescription information for each appointment
- **Relationships:**
  - One-to-One with Appointment (unique constraint)
  - One-to-Many with PrescriptionItem
- **Key Fields:**
  - `id` - Auto-generated primary key
  - `appointment` - Single reference to Appointment (UNIQUE constraint)
  - `diagnosis` - Diagnosis information (TEXT field)
  - `status` - "PENDING", "DISPENSED", "COMPLETED"
  - `createdAt` / `updatedAt` - Timestamps

**Cascade Behavior:**
```
cascade = CascadeType.ALL - All operations cascade to items
orphanRemoval = true - Deleting prescription deletes its items
```

**Important:**
- One appointment = One prescription (enforced by UNIQUE constraint)
- Deleting appointment cascades to prescription
- Deleting prescription cascades to prescription items

**Indexes:**
```
idx_prescription_appointment - Query by appointment
idx_prescription_status - Query by status filter
```

---

### 5. **PrescriptionItem** ⭐ NEW
- **Purpose:** Join entity linking prescriptions to medicines with quantities
- **Relationships:**
  - Many-to-One with Prescription (cascade PERSIST, MERGE)
  - Many-to-One with Medicine (NO cascade delete - just reference)
- **Key Fields:**
  - `id` - Auto-generated primary key
  - `prescription` - Reference to Prescription
  - `medicine` - Reference to Medicine
  - `quantityRequired` - How many units of medicine
  - `createdAt` / `updatedAt` - Timestamps

**Cascade Behavior:**
```
Prescription side: PERSIST, MERGE (create/update items with prescription)
Medicine side: PERSIST, MERGE only (never delete medicine with item)
```

**Indexes:**
```
idx_prescriptionitem_prescription - Query items by prescription
idx_prescriptionitem_medicine - Query items by medicine
```

---

## 🗄️ SQL Schema

### medicines table
```sql
CREATE TABLE medicines (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    stock INT NOT NULL,
    expiry_date DATE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,          -- Optimistic locking
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### appointments table
```sql
CREATE TABLE appointments (
    id BIGSERIAL PRIMARY KEY,
    doctor_id BIGINT NOT NULL,
    patient_id BIGINT NOT NULL REFERENCES patients(id),
    slot_timestamp TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### prescriptions table
```sql
CREATE TABLE prescriptions (
    id BIGSERIAL PRIMARY KEY,
    appointment_id BIGINT NOT NULL UNIQUE REFERENCES appointments(id),
    diagnosis TEXT,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### prescription_items table
```sql
CREATE TABLE prescription_items (
    id BIGSERIAL PRIMARY KEY,
    prescription_id BIGINT NOT NULL REFERENCES prescriptions(id),
    medicine_id BIGINT NOT NULL REFERENCES medicines(id),
    quantity_required INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## 🔄 Data Flow Example

### Scenario: Create Appointment with Prescription

```
1. Patient Books Appointment
   POST /api/appointments
   {
     "doctorId": 1,
     "patientId": 1,
     "slotTimestamp": "2026-02-20T10:00:00",
     "status": "BOOKED"
   }
   ✓ Appointment created, ID=1

2. Doctor Creates Prescription
   POST /api/prescriptions
   {
     "appointmentId": 1,
     "diagnosis": "Flu infection",
     "status": "PENDING",
     "items": [
       {"medicineId": 5, "quantityRequired": 10},
       {"medicineId": 7, "quantityRequired": 5}
     ]
   }
   ✓ Prescription created, ID=1
   ✓ PrescriptionItems created (2 items)

3. Pharmacy Dispenses Medicines
   PUT /api/prescriptions/1
   {
     "status": "DISPENSED"
   }
   ✓ Prescription status updated
   ✓ Medicine stock updated (optimistic locking prevents race conditions)

4. Query Patient's Prescriptions
   GET /api/patients/1/prescriptions
   ✓ Returns all prescriptions for patient 1
```

---

## 🎯 Key Features & Best Practices

### ✅ Optimistic Locking (Medicine.version)
```java
// Handles concurrent stock modifications
@Version
private Long version = 0L;

// Example: Two requests try to buy simultaneously
// Request 1 succeeds, Request 2 gets OptimisticLockException (retry)
```

### ✅ Cascade Operations
```java
// Prescription → PrescriptionItems
cascade = CascadeType.ALL
orphanRemoval = true

// Automatically deletes items when prescription is deleted
```

### ✅ Fetch Strategies
```java
// Appointment.patient - EAGER loading
@ManyToOne(fetch = FetchType.EAGER)
// Prevents N+1 queries when listing appointments

// Prescription.appointment - EAGER loading
@OneToOne(fetch = FetchType.EAGER)
// Automatically loads appointment details
```

### ✅ Database Constraints
```sql
-- Foreign key constraints with ON DELETE CASCADE
REFERENCES patients(id) ON DELETE CASCADE
REFERENCES appointments(id) ON DELETE CASCADE
REFERENCES prescriptions(id) ON DELETE CASCADE

-- Medicine: ON DELETE RESTRICT (prevent accidental deletion)
REFERENCES medicines(id) ON DELETE RESTRICT
```

### ✅ Indexes for Performance
```sql
-- Fast lookups by common query criteria
idx_medicine_name
idx_medicine_expiry
idx_appointment_doctor
idx_appointment_patient
idx_appointment_slot
idx_appointment_status
idx_prescription_appointment
idx_prescription_status
idx_prescriptionitem_prescription
idx_prescriptionitem_medicine
```

---

## 📝 Repository Query Examples

### MedicineRepository
```java
// Find expiring medicines
List<Medicine> expiringMeds = medicineRepo.findExpiringMedicines(LocalDate.now().plusDays(30));

// Find low stock items
List<Medicine> lowStock = medicineRepo.findLowStockMedicines(5);

// Search by name
List<Medicine> results = medicineRepo.searchByName("paracetamol");
```

### AppointmentRepository
```java
// Check if doctor slot is available
boolean booked = appointmentRepo.isSlotBooked(doctorId, slotTime);

// Find doctor's booked appointments
List<Appointment> appointments = appointmentRepo.findBookedAppointmentsByDoctorId(doctorId);

// Find appointments in date range
List<Appointment> range = appointmentRepo.findAppointmentsBetween(start, end);
```

### PrescriptionRepository
```java
// Find prescription for appointment
Prescription rx = prescriptionRepo.findByAppointmentId(appointmentId).orElse(null);

// Get all pending prescriptions
List<Prescription> pending = prescriptionRepo.findPendingPrescriptions();

// Count pending for patient
Long count = prescriptionRepo.countPendingByPatientId(patientId);
```

### PrescriptionItemRepository
```java
// Get all items for prescription
List<PrescriptionItem> items = itemRepo.findByPrescriptionId(prescriptionId);

// Check stock availability
List<PrescriptionItem> shortages = itemRepo.findItemsWithInsufficientStock(medicineId);

// Total quantity needed from all pending prescriptions
Integer totalNeeded = itemRepo.getTotalQuantityRequiredForMedicine(medicineId);
```

---

## 🚀 DTOs (Data Transfer Objects)

### Request DTOs (Validation)
- `MedicineRequestDTO` - Create/Update medicine
- `AppointmentRequestDTO` - Book appointment
- `PrescriptionRequestDTO` - Create prescription with items
- `PrescriptionItemRequestDTO` - Prescription item line items

### Response DTOs (API Output)
- `MedicineResponseDTO` - Return medicine info with version
- `AppointmentResponseDTO` - Return appointment with patient details
- `PrescriptionResponseDTO` - Return prescription with items
- `PrescriptionItemResponseDTO` - Return item with medicine and stock info

---

## 📋 Migration Strategy

### Chat 1: Initial Schema (V1)
- `patients` table created

### Chat 4: Clinic Management (V2) - THIS CHAT
- `medicines` table
- `appointments` table
- `prescriptions` table
- `prescription_items` table

**Migration File:** `src/main/resources/db/migration/V2__Create_clinic_management_tables.sql`

---

## ⚠️ Important Notes

1. **Version Column (Medicine)**
   - DO NOT update manually
   - Used by Hibernate for optimistic locking
   - Prevents concurrent stock modification issues

2. **Cascade Behavior**
   - Prescription deletes → Cascade to PrescriptionItems
   - Appointment deletes → Cascade to Prescription → Cascade to Items
   - Medicine deletes → RESTRICTED (can't delete if referenced)

3. **One-to-One Relationship**
   - Appointment → Prescription is 1:1
   - UNIQUE constraint on appointment_id prevents duplicates
   - If appointment is deleted, prescription is auto-deleted

4. **Query Performance**
   - Always use indexes on frequently queried columns
   - EAGER fetch for relationships used in API responses
   - LAZY fetch for relationships rarely accessed

---

## 🔍 Validation Rules

### Medicine
- Name: Required, not blank
- Stock: Required, positive integer
- Expiry Date: Required, valid date

### Appointment
- Doctor ID: Required, positive integer
- Patient ID: Required, positive integer, must exist
- Slot Timestamp: Required, valid future timestamp
- Status: Required, one of: BOOKED, COMPLETED, CANCELLED

### Prescription
- Appointment ID: Required, positive integer, must exist
- Status: Required, one of: PENDING, DISPENSED, COMPLETED
- Items: Required, not empty array

### PrescriptionItem
- Medicine ID: Required, positive integer, must exist
- Quantity Required: Required, positive integer

---

**Version:** 1.0  
**Last Updated:** February 18, 2026  
**Status:** ✅ Production Ready
