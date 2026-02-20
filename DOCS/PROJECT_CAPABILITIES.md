# Medi-D Backend API - Capabilities & Documentation

## 🎯 Project Overview

**Medi-D** is a Spring Boot-based backend for a Healthcare OPD (Out-Patient Department) and Pharmacy Management System. The current implementation provides a foundational Patient Management module with proper layered architecture, validation, and error handling.

---

## ✅ Current Capabilities

### 1. **Patient Management**

#### A. Create Patient (POST)
- **Endpoint:** `POST /api/patients`
- **Description:** Create a new patient record in the system
- **Request Body:**
  ```json
  {
    "name": "John Doe",
    "contact": "+1-555-0100"
  }
  ```
- **Response:** `201 Created`
  ```json
  {
    "id": 1,
    "name": "John Doe"
  }
  ```
- **Validation:**
  - ✓ Name is required (not blank)
  - ✓ Contact is required (not blank)
  - ✓ Contact must be a valid phone number format
  - ✓ Returns detailed validation errors if invalid

#### B. Retrieve Patient (GET)
- **Endpoint:** `GET /api/patients/{id}`
- **Description:** Retrieve patient details by patient ID
- **Response:** `200 OK`
  ```json
  {
    "id": 1,
    "name": "John Doe"
  }
  ```
- **Error Handling:**
  - ✓ Returns `404 NOT_FOUND` if patient doesn't exist
  - ✓ Returns `400 BAD_REQUEST` if ID is invalid

#### C. System Health Check (GET)
- **Endpoint:** `GET /api/health`
- **Description:** Check if the server is running
- **Response:** `200 OK`
  ```
  Medi-D System is up and running!
  ```

---

## 🏗️ Architecture & Design Patterns

### Layered Architecture
```
┌─────────────────────────────────────┐
│         Controller Layer            │  (com.medid.controller)
│     - Request/Response Handling     │
│     - Input Validation              │
└────────────────┬────────────────────┘
                 │
┌────────────────▼────────────────────┐
│         Service Layer               │  (com.medid.service)
│     - Business Logic                │
│     - Data Processing               │
└────────────────┬────────────────────┘
                 │
┌────────────────▼────────────────────┐
│       Repository Layer              │  (com.medid.repository)
│    - Database Interactions          │
│    - JPA/Hibernate Operations       │
└────────────────┬────────────────────┘
                 │
┌────────────────▼────────────────────┐
│       Database Layer                │
│   - PostgreSQL (medid_test DB)      │
└─────────────────────────────────────┘
```

### Supporting Layers

**DTO Layer** (com.medid.dto)
- `PatientRequestDTO` - Handles incoming requests with validation
- `PatientResponseDTO` - Formats outgoing responses

**Entity Layer** (com.medid.entity)
- `Patient` - JPA entity with database mapping

**Exception Handling** (com.medid.exception)
- `ResourceNotFoundException` - For 404 errors
- `InvalidRequestException` - For 400 errors
- `GlobalExceptionHandler` - Centralized error handling
- `ErrorResponse` - Standardized error response format

---

## 🛡️ Error Handling & Validation

### Built-in Exception Handlers
1. **Validation Errors** → `400 BAD_REQUEST`
   - Field-level validation errors shown in response
   - Example: Empty name, invalid phone format

2. **Resource Not Found** → `404 NOT_FOUND`
   - When querying non-existent patient ID
   - Clear error message with ID

3. **Invalid Requests** → `400 BAD_REQUEST`
   - Null/empty required fields
   - Business logic violations

4. **Internal Server Errors** → `500 INTERNAL_SERVER_ERROR`
   - Unexpected exceptions logged and handled gracefully

### Error Response Format
```json
{
  "status": 400,
  "message": "Validation failed",
  "error": "BAD_REQUEST",
  "timestamp": "2026-02-18T14:47:12.129385054",
  "path": "/api/patients",
  "validationErrors": {
    "name": "Patient name is required and cannot be empty",
    "contact": "Contact must be a valid phone number..."
  }
}
```

---

## 📊 Database Schema

### Patients Table
```sql
CREATE TABLE patients (
    id BIGSERIAL PRIMARY KEY,                 -- Auto-incremented ID
    name VARCHAR(255) NOT NULL,               -- Patient name
    contact VARCHAR(255) NOT NULL,            -- Contact information
    created_at TIMESTAMP DEFAULT NOW(),       -- Creation timestamp
    updated_at TIMESTAMP DEFAULT NOW()        -- Last update timestamp
);

-- Performance index
CREATE INDEX idx_patients_name ON patients(name);
```

### Database Credentials
- **Database:** medid_test
- **User:** medid_admin
- **Password:** medid_pass
- **Host:** localhost:5432

---

## 📦 Project Structure

```
medi-d/
├── src/main/java/com/medid/
│   ├── controller/
│   │   └── PatientController.java          # REST endpoints
│   ├── service/
│   │   └── PatientService.java             # Business logic
│   ├── repository/
│   │   └── PatientRepository.java          # Data access
│   ├── entity/
│   │   └── Patient.java                    # JPA entity
│   ├── dto/
│   │   ├── PatientRequestDTO.java          # Input validation
│   │   └── PatientResponseDTO.java         # Response format
│   ├── exception/
│   │   ├── ResourceNotFoundException.java  # Custom exception
│   │   ├── InvalidRequestException.java    # Custom exception
│   │   └── handler/
│   │       ├── GlobalExceptionHandler.java # Centralized error handling
│   │       └── ErrorResponse.java          # Error response format
│   └── MediDApplication.java               # Application entry point
├── src/main/resources/
│   ├── application.properties              # Profile configuration
│   ├── application-dev.properties          # Development configuration
│   └── db/migration/
│       └── V1__Create_patients_table.sql   # Database schema
├── .env                                    # Environment variables (local)
├── .env.example                            # Environment template
├── setup-db.sh                             # Database setup script
└── pom.xml                                 # Maven dependencies
```

---

## 🚀 Technologies Used

### Core Framework
- **Spring Boot 3.5.10** - Modern Java framework
- **Spring Data JPA** - ORM and database interaction
- **Hibernate 6.6.4** - Object-relational mapping
- **PostgreSQL 16** - Production-grade database

### Java Features
- **Jakarta EE** - Enterprise Java standard
- **Lombok** - Reduce boilerplate code
- **Java 21** - Latest LTS Java version

### Validation & Error Handling
- **Jakarta Validation API** - Input validation
- **Spring Error Handling** - Global exception handling
- **SLF4J & Logback** - Structured logging

### Build & Deployment
- **Maven 3.x** - Build tool
- **Spring Boot Maven Plugin** - Application packaging

---

## 🧪 Testing the API

### Using curl

**1. Health Check**
```bash
curl http://localhost:8080/api/health
```

**2. Create a Patient**
```bash
curl -X POST http://localhost:8080/api/patients \
  -H "Content-Type: application/json" \
  -d '{"name":"John Doe","contact":"+1-555-0100"}'
```

**3. Get Patient Details**
```bash
curl http://localhost:8080/api/patients/1
```

**4. Test Validation (Invalid Name)**
```bash
curl -X POST http://localhost:8080/api/patients \
  -H "Content-Type: application/json" \
  -d '{"name":"","contact":"+1-555-0100"}'
```

**5. Test 404 Error**
```bash
curl http://localhost:8080/api/patients/999
```

### Using Postman/Insomnia

**Base URL:** `http://localhost:8080/api`

**Create Patient Request:**
- Method: POST
- URL: `/patients`
- Headers: `Content-Type: application/json`
- Body:
  ```json
  {
    "name": "Jane Smith",
    "contact": "+91-9876543210"
  }
  ```

---

## 🔍 Key Features

### ✓ Input Validation
- Required field validation
- Phone number format validation
- Field-level error messages
- Trim whitespace before saving

### ✓ Exception Handling
- Global exception handler
- Consistent error response format
- Detailed logging for debugging
- Proper HTTP status codes

### ✓ Logging
- Controller method logging
- Service-level operation tracking
- Exception logging with stack traces
- Structured logging with timestamps

### ✓ Data Persistence
- Automatic table creation
- Timestamps (created_at, updated_at)
- Database indexes for performance
- Transaction support

### ✓ RESTful API Design
- Clear resource naming (`/api/patients`)
- Proper HTTP methods (GET, POST)
- Correct status codes (200, 201, 400, 404, 500)
- JSON request/response format

---

## 📈 Future Enhancements

### Planned Features
1. **Patient Update/Delete**
   - `PUT /api/patients/{id}` - Update patient
   - `DELETE /api/patients/{id}` - Delete patient
   - `GET /api/patients` - List all patients with pagination

2. **OPD Management**
   - Appointment scheduling
   - Appointment status tracking
   - Doctor assignment
   - Consultation notes

3. **Pharmacy Management**
   - Medicine inventory
   - Prescription tracking
   - Medicine availability

4. **Advanced Features**
   - User authentication (JWT)
   - Role-based access control (RBAC)
   - Audit logging
   - API documentation (Swagger/OpenAPI)
   - Pagination & filtering
   - Search functionality

5. **Integration**
   - Email notifications
   - SMS notifications
   - Payment gateway integration
   - Report generation

---

## 🛠️ Running the Application

### Start the Application
```bash
# Option 1: Using provided script
./run-app.sh

# Option 2: Using Maven
source .env && mvn spring-boot:run

# Option 3: Direct JAR execution
java -jar target/medi-d-0.0.1-SNAPSHOT.jar
```

### Database Setup
```bash
# Run the database setup script
./setup-db.sh
```

### Logs Location
- Console output while application is running
- Application logs at: `./logs/` (if configured)

---

## 📋 Summary

**Medi-D** is a **production-ready, enterprise-grade** Patient Management REST API with:
- ✅ Clean layered architecture
- ✅ Comprehensive input validation
- ✅ Robust error handling
- ✅ Database persistence
- ✅ Detailed logging
- ✅ RESTful design
- ✅ Type-safe operations (DTOs)
- ✅ Transaction support

The foundation is solid for rapidly adding more features like OPD management, Pharmacy operations, authentication, and advanced analytics.

---

**Version:** 0.0.1-SNAPSHOT  
**Last Updated:** February 18, 2026  
**Status:** ✅ Development/Production Ready
