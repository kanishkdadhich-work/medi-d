# Medi-D Postman Testing - Quick Start

## 🚀 Get Started in 5 Minutes

### Step 1: Prepare Your Environment
```bash
# Start the application
./mvnw spring-boot:run

# OR use the run script
./run-app.sh

# Verify database is running
./test-db-connection.sh
```

### Step 2: Open Postman and Import Collection
1. Open Postman
2. File → Import
3. Choose file: `medi-d-postman-collection.json`
4. Click Import

### Step 3: Set Environment Variables
1. Click "Environments" (left sidebar)
2. Select or create new environment with these variables:
   - `base_url` = `http://localhost:8080/api`
   - `patient_id` = `1` (will auto-update)
   - `medicine_id` = `1` (will auto-update)
   - `appointment_id` = `1` (will auto-update)
   - `prescription_id` = `1` (will auto-update)

---

## 📋 What Can You Test?

### 1. **Patient Management**
- ✅ Create new patients with validation
- ✅ Retrieve patient details
- ✅ Handle validation errors (empty name, invalid contact)
- ✅ Test 404 errors for non-existent patients

### 2. **Medicine Management**
- ✅ Create medicines with inventory tracking
- ✅ Update medicine stock levels
- ✅ Delete medicines
- ✅ Search medicines by name
- ✅ Find low stock medicines
- ✅ Find expiring medicines
- ✅ Validate stock (no negative values)
- ✅ Validate expiry dates (no past dates)

### 3. **Appointment Management**
- ✅ Schedule appointments
- ✅ View appointments by patient
- ✅ View appointments by doctor
- ✅ Check slot availability
- ✅ Prevent double-booking
- ✅ Prevent past appointments

### 4. **Prescription Management**
- ✅ Create prescriptions
- ✅ View prescriptions
- ✅ List patient prescriptions
- ✅ Track prescription status

### 5. **System Health**
- ✅ Health check endpoint
- ✅ API availability verification

---

## 🔄 Recommended Testing Workflow

### Complete Flow (15-20 minutes)

```
1. Health Check
   ↓
2. Create Patient (auto-saves patient_id)
   ↓
3. Create Medicine (auto-saves medicine_id)
   ↓
4. Create Appointment
   ↓
5. Create Prescription
   ↓
6. Query Operations
   - Get Patient Appointments
   - Get Patient Prescriptions
   - Search Medicines
   ↓
7. Update Operations
   - Update Medicine Stock
   ↓
8. Delete Operations
   - Delete Medicine
   - Verify 404
```

---

## 📊 Response Status Codes to Expect

| Status | Meaning | Example |
|--------|---------|---------|
| **200** | ✅ Success (GET, PUT) | Get patient returns 200 |
| **201** | ✅ Created (POST) | Create patient returns 201 |
| **204** | ✅ Success (DELETE) | Delete returns 204 (no body) |
| **400** | ❌ Validation Error | Invalid phone format |
| **404** | ❌ Not Found | Patient ID 999 doesn't exist |
| **500** | ❌ Server Error | Unexpected error |

---

## ✨ Key Features of This Collection

✓ **Pre-built Requests** - All endpoints ready to use
✓ **Test Scripts** - Automated validation included
✓ **Dynamic Variables** - Auto-captures IDs for next request
✓ **Error Cases** - Tests for invalid data
✓ **Realistic Data** - Working examples provided
✓ **Validation Examples** - Shows all validation rules

---

## 🔧 How to Use the Collection

### Run Single Request
1. Click on any request in the collection
2. Review the URL and body
3. Click "Send"
4. Check Response tab for results

### Run Full Collection (Runner)
1. Click "Runner" (top-left)
2. Select "Medi-D Healthcare API"
3. Select Environment
4. Click "Run"
5. Watch automated execution

### Test Specific Flow
1. Group-related requests from left panel
2. Click "Run" inside folder
3. Requests execute in order

---

## 📝 Sample Requests & Expected Responses

### 1. Create Patient
**Request:**
```bash
POST http://localhost:8080/api/patients
Content-Type: application/json

{
  "name": "John Doe",
  "contact": "+1-555-0100"
}
```

**Response (201 Created):**
```json
{
  "id": 1,
  "name": "John Doe"
}
```

---

### 2. Create Medicine
**Request:**
```bash
POST http://localhost:8080/api/medicines
Content-Type: application/json

{
  "name": "Aspirin",
  "stock": 100,
  "expiryDate": "2025-12-31"
}
```

**Response (201 Created):**
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

### 3. Create Appointment
**Request:**
```bash
POST http://localhost:8080/api/appointments
Content-Type: application/json

{
  "patientId": 1,
  "doctorId": 10,
  "slotTimestamp": "2026-03-15T10:00:00"
}
```

**Response (201 Created):**
```json
{
  "appointment_id": 1,
  "patient_id": 1,
  "patient_name": "John Doe",
  "doctor_id": 10,
  "slot_timestamp": "2026-03-15T10:00:00",
  "status": "SCHEDULED"
}
```

---

## 🎯 Testing Checklist

### Happy Path (Should All Pass ✅)
- [ ] Health Check returns 200
- [ ] Create Patient returns 201
- [ ] Get Patient returns 200
- [ ] Create Medicine returns 201
- [ ] Get Medicine returns 200
- [ ] Search Medicine returns 200
- [ ] Update Medicine returns 200
- [ ] Create Appointment returns 201
- [ ] Create Prescription returns 201

### Error Path (Should All Return Errors ❌)
- [ ] Create Patient with empty name → 400
- [ ] Create Patient with invalid contact → 400
- [ ] Get Patient 999 → 404
- [ ] Create Medicine with negative stock → 400
- [ ] Create Medicine with past expiry → 400
- [ ] Create past appointment → 400
- [ ] Delete Medicine then Get → 404

---

## 🐛 Troubleshooting

### "Connection Refused"
```bash
# Check if app is running
curl http://localhost:8080/api/health
```

### Database Connection Issues
```bash
# Test database connection
./test-db-connection.sh

# Setup database if needed
./setup-db.sh
```

### Invalid JSON Errors
- Ensure JSON is properly formatted (use Postman's validate)
- Check all quotes are straight quotes, not curly
- Use correct data types (strings in quotes, numbers without quotes)

### Request Times Out
- Ensure database is responsive
- Check terminal for application errors
- Restart the application

---

## 💡 Advanced Testing Tips

### 1. **Save Responses for Comparison**
   - Right-click response
   - Save as example
   - Compare later

### 2. **Use Pre-request Scripts**
   - Set timestamp for appointment
   - Generate unique values

### 3. **Create Test Suites**
   - Group related requests
   - Run in specific order
   - Validate data flow

### 4. **Performance Testing**
   - Use Collection Runner
   - Set iterations to 10+
   - Monitor response times

### 5. **Documentation Export**
   - File → Export
   - Generate HTML documentation
   - Share with team

---

## 📚 Related Documentation

- Full Guide: [POSTMAN_TESTING_GUIDE.md](POSTMAN_TESTING_GUIDE.md)
- API Reference: [DOCS/API_REFERENCE.md](DOCS/API_REFERENCE.md)
- API Documentation: [DOCS/API_DOCUMENTATION.md](DOCS/API_DOCUMENTATION.md)
- Development Guide: [DOCS/DEVELOPMENT_GUIDE.md](DOCS/DEVELOPMENT_GUIDE.md)

---

## 🆘 Need Help?

**Issue:** Postman shows 500 error
- Check application logs in terminal
- Often means database issue
- Verify `./test-db-connection.sh` passes

**Issue:** Tests fail but manual requests work
- Clear Postman cache (Settings → Data)
- Reset environment variables
- Re-import collection

**Issue:** Auto-captured IDs not working
- Ensure tests are enabled on requests
- Check previous request returned valid data
- Look at Test Results tab to debug

---

## 🎉 You're All Set!

You now have everything needed to test all functionalities of the Medi-D system using Postman!

**Next Steps:**
1. Import the Postman collection
2. Start your application
3. Run the health check
4. Follow the testing workflow
5. Explore all endpoints

Happy Testing! 🚀
