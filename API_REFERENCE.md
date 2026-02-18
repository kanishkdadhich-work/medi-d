# Medi-D API Quick Reference

## Base URL
```
http://localhost:8080/api
```

## Endpoints

### 1. Health Check
```
GET /health
Response: 200 OK
Body: "Medi-D System is up and running!"
```

### 2. Create Patient
```
POST /patients
Content-Type: application/json

Request Body:
{
  "name": "John Doe",
  "contact": "+1-555-0100"
}

Success Response: 201 Created
{
  "id": 1,
  "name": "John Doe"
}

Validation Error Response: 400 Bad Request
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
```

### 3. Get Patient
```
GET /patients/{id}

Success Response: 200 OK
{
  "id": 1,
  "name": "John Doe"
}

Error Response: 404 Not Found
{
  "status": 404,
  "message": "Patient not found with ID: 999",
  "error": "NOT_FOUND",
  "timestamp": "2026-02-18T14:47:25.701194639",
  "path": "/api/patients/999",
  "validationErrors": null
}
```

## Validation Rules

### Patient Name
- ✓ Required (cannot be empty or blank)
- ✓ White spaces are trimmed before saving
- ✓ Maximum: 255 characters (enforced by database)

### Patient Contact
- ✓ Required (cannot be empty or blank)
- ✓ Must be a valid phone number format
- ✓ Accepts formats like:
  - +1-555-0100
  - +91-9876543210
  - 5550100
  - +1 (555) 0100
  - +1.555.0100

## HTTP Status Codes

| Code | Meaning | When |
|------|---------|------|
| 200 | OK | Successful GET request |
| 201 | Created | Patient successfully created |
| 400 | Bad Request | Validation failed or invalid data |
| 404 | Not Found | Patient ID doesn't exist or endpoint not found |
| 500 | Internal Server Error | Unexpected server error |

## cURL Examples

### Health Check
```bash
curl http://localhost:8080/api/health
```

### Create Patient
```bash
curl -X POST http://localhost:8080/api/patients \
  -H "Content-Type: application/json" \
  -d '{"name":"John Doe","contact":"+1-555-0100"}'
```

### Get Patient
```bash
curl http://localhost:8080/api/patients/1
```

### Get Patient (pretty print)
```bash
curl http://localhost:8080/api/patients/1 | jq .
```

### Create Patient (pretty print response)
```bash
curl -X POST http://localhost:8080/api/patients \
  -H "Content-Type: application/json" \
  -d '{"name":"Jane Smith","contact":"+91-9876543210"}' | jq .
```

## Error Scenarios

### 1. Empty Name
```bash
curl -X POST http://localhost:8080/api/patients \
  -H "Content-Type: application/json" \
  -d '{"name":"","contact":"+1-555-0100"}'
```
Response: 400 Bad Request with validation error

### 2. Invalid Contact Format
```bash
curl -X POST http://localhost:8080/api/patients \
  -H "Content-Type: application/json" \
  -d '{"name":"John Doe","contact":"invalid"}'
```
Response: 400 Bad Request with validation error

### 3. Missing Fields
```bash
curl -X POST http://localhost:8080/api/patients \
  -H "Content-Type: application/json" \
  -d '{"name":"John Doe"}'
```
Response: 400 Bad Request - contact is required

### 4. Non-existent Patient ID
```bash
curl http://localhost:8080/api/patients/99999
```
Response: 404 Not Found - Patient not found

## Database Information

**Patient Data Storage:**
```sql
-- View all patients
SELECT id, name, contact, created_at, updated_at FROM patients;

-- View specific patient
SELECT * FROM patients WHERE id = 1;

-- Count total patients
SELECT COUNT(*) as total_patients FROM patients;
```

## Troubleshooting

### Application won't start
1. Check if PostgreSQL is running: `pg_isready -h localhost -p 5432`
2. Verify .env file has correct credentials
3. Ensure database and user exist: `./setup-db.sh`

### Connection refused error
1. Verify PostgreSQL service is running
2. Check DB_HOST and DB_PORT in .env
3. Restart PostgreSQL service

### Validation errors
1. Check field values are not empty/blank
2. Ensure contact follows phone number format
3. Review error response for specific field issues

### Ports already in use
1. Change port in application-dev.properties: `server.port=8081`
2. Or kill existing process: `lsof -i :8080`
