# Medi-D Postman Authentication - Quick Reference

## 🎯 What to Do RIGHT NOW

### Step 1: Start Your Application
```bash
./mvnw spring-boot:run
```
Wait for: `"Started MediDApplication in X seconds"`

### Step 2: Import the New Authenticated Collection
1. Open Postman
2. **File → Import**
3. Choose: `medi-d-authenticated-postman-collection.json`
4. Click **Import**
5. You now have a collection with built-in authentication!

---

## 🔐 The Three Test Users (In-Memory)

```
╔══════════════════════════════════════════════════════╗
║           PRE-LOADED TEST USERS                      ║
╠═════════════════════╦════════════════╦═══════════════╣
║ Username            ║ Password       ║ Role          ║
╠═════════════════════╬════════════════╬═══════════════╣
║ receptionist        ║ receptpass     ║ RECEPTIONIST  ║
║ doctor              ║ doctorpass     ║ DOCTOR        ║
║ pharmacist          ║ pharmacistpass ║ PHARMACIST    ║
╚═════════════════════╩════════════════╩═══════════════╝
```

---

## 🚀 Quick Start Workflow

### 5-Minute Test (Everything)

```
STEP 1: Login as Each Role (3 min)
├─ Click: "1️⃣ Login as Receptionist" → Send
│  ✓ See: Token auto-saved to {{receptionist_token}}
├─ Click: "2️⃣ Login as Doctor" → Send  
│  ✓ See: Token auto-saved to {{doctor_token}}
└─ Click: "3️⃣ Login as Pharmacist" → Send
   ✓ See: Token auto-saved to {{pharmacist_token}}

STEP 2: Test Receptionist Tasks (1 min)
├─ Click: "Register New Patient" → Send
│  ✓ See: 201 Created, patient_id saved
├─ Click: "Book Appointment" → Send
│  ✓ See: 201 Created
└─ Click: "❌ Test: Pharmacist Can't Register Patient" → Send
   ✓ See: 403 Forbidden (as expected)

STEP 3: Test Doctor Tasks (1 min)
├─ Click: "View My Appointments" → Send
│  ✓ See: 200 OK, list of appointments
├─ Click: "Create Prescription" → Send
│  ✓ See: 201 Created
└─ Click: "❌ Test: Receptionist Can't Dispense" → Send
   ✓ See: 403 Forbidden (as expected)

STEP 4: Test Pharmacist Tasks (1 min)
├─ Click: "View Pending Prescriptions" → Send
│  ✓ See: 200 OK (patient diagnosis hidden)
├─ Click: "Check Medicine Stock" → Send
│  ✓ See: 200 OK
└─ Click: "Dispense Prescription" → Send
   ✓ See: 200 OK (if stock available)

STEP 5: Verify Security (1 min)
├─ Click: "❌ Test: No Token" → Send
│  ✓ See: 401 or 403 Unauthorized
├─ Click: "❌ Test: Invalid Credentials" → Send
│  ✓ See: 401 Unauthorized
└─ Click: "❌ Test: Wrong Role for Endpoint" → Send
   ✓ See: 403 Forbidden
```

---

## 📊 How the NEW Collection Works

### Automatic Token Capture

```
LOGIN REQUEST
    ↓
POST /api/auth/login (with username/password)
    ↓
RESPONSE: {"token": "eyJhbGciOiJI..."}
    ↓
TEST SCRIPT RUNS
    pm.environment.set('receptionist_token', jsonData.token)
    ↓
TOKEN SAVED IN ENVIRONMENT
    {{receptionist_token}} = "eyJhbGciOiJI..."
    ↓
USE IN OTHER REQUESTS
    Authorization: Bearer {{receptionist_token}}
```

### Automatic Usage in Requests

Every protected endpoint now has this header pre-configured:
```
Authorization: Bearer {{receptionist_token}}
                       ← Automatically replaced with actual token
```

You don't need to copy/paste! It's automatic!

---

## 📋 Complete Functionality Matrix

### ✅ What's Been Implemented

| Feature | Status | How to Test |
|---------|--------|----------|
| **JWT Authentication** | ✅ Ready | POST /api/auth/login |
| **Role-Based Access** | ✅ Ready | Try to access endpoint with wrong role |
| **Receptionist: Register Patient** | ✅ Ready | POST /api/patients (receptionist token) |
| **Receptionist: Book Appointment** | ✅ Ready | POST /api/appointments (receptionist token) |
| **Doctor: View Appointments** | ✅ Ready | GET /api/appointments/doctor/10 (doctor token) |
| **Doctor: Create Prescription** | ✅ Ready | POST /api/prescriptions (doctor token) |
| **Pharmacist: View Pending** | ✅ Ready | GET /api/pharmacy/prescriptions/pending (pharmacist token) |
| **Pharmacist: Dispense** | ✅ Ready | POST /api/pharmacy/.../dispense (pharmacist token) |
| **Privacy Protection** | ✅ Ready | Doctor diagnosis hidden from pharmacist |
| **Token Expiration** | ✅ Ready | Wait 1 hour for token to expire |
| **Rate Limiting** | ✅ Ready | Multiple rapid requests will be throttled |

---

## 🎓 Step-by-Step Instructions

### How to Login

**1. Open the Collection**
- Click folder: "🔐 AUTHENTICATION"

**2. Click "1️⃣ Login as Receptionist"**
```
POST http://localhost:8080/api/auth/login
Body: {"username": "receptionist", "password": "receptpass"}
```

**3. See the Test Script at Bottom**
```javascript
pm.environment.set('receptionist_token', jsonData.token);
console.log('✓ Receptionist token saved to environment');
```

**4. Click "Send"**
- You'll see response with token
- Test script automatically saves it
- No manual copying needed!

---

### How to Make Authenticated Request

**1. Open "Register New Patient"**
```
POST /api/patients
Authorization: Bearer {{receptionist_token}}
```

**2. Notice the Headers Tab**
- Authorization header is pre-filled
- {{receptionist_token}} will be replaced automatically
- You login once, use everywhere!

**3. Click "Send"**
- The system replaces {{receptionist_token}} with actual token
- Request succeeds with 201 Created

---

## 🔄 Token Flow Diagram

```
LOGIN ENDPOINT
│
├─ Execute Test Script
│  pm.environment.set('receptionist_token', token)
│
ENVIRONMENT VARIABLE UPDATED
│
{{receptionist_token}} = "eyJhbGciOiJIUzI1NiIs..."

USE IN ANY REQUEST
│
Authorization: Bearer {{receptionist_token}}
                       ↓
                    Replaced with actual token
                       ↓
                    Request sent with auth
                       ↓
                    API validates & responds
```

---

## 📝 Common Workflows to Test

### Workflow 1: Complete Patient Journey

```
1. [RECEPTIONIST] Login
   → GET receptionist_token

2. [RECEPTIONIST] Register Patient
   → GET patient_id = 1

3. [RECEPTIONIST] Book Appointment  
   → GET appointment_id = 1

4. [DOCTOR] Login
   → GET doctor_token

5. [DOCTOR] View Patient Appointments
   → See appointment_id = 1

6. [DOCTOR] Create Prescription
   → GET prescription_id = 1

7. [PHARMACIST] Login
   → GET pharmacist_token

8. [PHARMACIST] View Pending
   → See prescription_id = 1 (diagnosis hidden!)

9. [PHARMACIST] Dispense
   → Mark as dispensed

10. [RECEPTIONIST] Can't Dispense
    → TRY to dispense with receptionist token
    → GET 403 Forbidden ✓
```

### Workflow 2: Security Verification

```
1. Try endpoint WITHOUT token
   → GET 401 or 403 Unauthorized ✓

2. Try endpoint WITH WRONG TOKEN
   → GET 403 Forbidden ✓

3. Try endpoint WITH EXPIRED TOKEN
   → GET 401 Unauthorized ✓

4. Try endpoint WITH WRONG ROLE
   → GET 403 Forbidden ✓

5. Try login WITH WRONG PASSWORD
   → GET 401 Unauthorized ✓
```

---

## 🔑 Environment Variables (Auto-Managed)

The collection uses these variables (auto-saved):

| Variable | Set By | Used For |
|----------|--------|----------|
| `base_url` | Manual | All URLs (`http://localhost:8080/api`) |
| `receptionist_token` | Login endpoint test | Receptionist requests |
| `doctor_token` | Login endpoint test | Doctor requests |
| `pharmacist_token` | Login endpoint test | Pharmacist requests |
| `patient_id` | Register patient test | Patient-specific requests |
| `appointment_id` | Book appointment test | Appointment-specific requests |
| `prescription_id` | Create prescription test | Prescription-specific requests |

**You don't need to manually set these!** The test scripts do it automatically.

---

## ✨ Key Features of This Collection

✅ **Pre-configured login for all 3 roles**
✅ **Automatic token capture via test scripts**
✅ **Auto-save to environment variables**
✅ **All requests pre-configured with Bearer tokens**
✅ **Error cases included (403, 401, etc.)**
✅ **Real-world workflow examples**
✅ **Security verification tests**
✅ **Comments on every request**

---

## 🛠️ Postman Environment Setup

The collection comes with these default variables (you only need to import!):

```json
{
  "variables": [
    {
      "key": "base_url",
      "value": "http://localhost:8080/api"
    },
    {
      "key": "receptionist_token",
      "value": ""  ← Will be filled by login
    },
    {
      "key": "doctor_token",
      "value": ""  ← Will be filled by login
    },
    {
      "key": "pharmacist_token",
      "value": ""  ← Will be filled by login
    }
  ]
}
```

---

## 🐛 Troubleshooting

### Problem: "Authorization header was either missing or invalid"
**Cause:** Forgot to login first
**Solution:**
1. Go to "🔐 AUTHENTICATION" folder
2. Click "Login as Receptionist"
3. Click "Send"
4. Token is now saved

### Problem: "403 Forbidden"
**Cause:** Wrong role for endpoint
**Solution:**
1. Check endpoint requirements (should be listed in description)
2. Make sure you're using correct token:
   - Patient registration? Use {{receptionist_token}}
   - Doctor appointments? Use {{doctor_token}}
   - Dispensing? Use {{pharmacist_token}}

### Problem: "401 Unauthorized"
**Cause:** Token expired or invalid
**Solution:**
1. Re-login to get fresh token
2. Token lasts 1 hour by default
3. After 1 hour, login again

### Problem: "Cannot connect to server"
**Cause:** Application not running
**Solution:**
```bash
./mvnw spring-boot:run
# Wait for "Started MediDApplication in X seconds"
```

---

## 📊 Two Collections Available

You now have TWO collections:

### 1. **medi-d-postman-collection.json** (Original)
- No authentication
- Simple endpoints
- Good for basic testing
- Works without login

### 2. **medi-d-authenticated-postman-collection.json** (NEW) ⭐
- JWT authentication
- Role-based access
- Automatic token management
- Use THIS ONE for role-based testing

**Recommendation:** Use the NEW authenticated collection for all testing!

---

## 🎯 Next Steps

### Immediate (Next 5 minutes)
1. ✅ Import `medi-d-authenticated-postman-collection.json`
2. ✅ Run "Login as Receptionist"
3. ✅ Run "Register New Patient"
4. ✅ Run "Book Appointment"

### Short Term (Next 30 minutes)
1. ✅ Test all three roles
2. ✅ Verify role restrictions (403 errors)
3. ✅ Test security (401 without token)
4. ✅ Verify token replacement

### Medium Term (Next 2 hours)
1. ✅ Complex workflows
2. ✅ Concurrency testing (two simultaneous requests)
3. ✅ All-or-nothing dispensing verification
4. ✅ Privacy verification (diagnosis hidden)

---

## ✅ Success Checklist

After testing, you should see:

- ✅ Login returns JWT token
- ✅ Token is auto-saved to environment
- ✅ Protected endpoints require token
- ✅ Receptionist can register patient
- ✅ Receptionist can book appointment
- ✅ Doctor can view appointments
- ✅ Doctor can create prescription
- ✅ Pharmacist can see pending
- ✅ Pharmacist can dispense
- ✅ Pharmacist can't see diagnosis (privacy)
- ✅ Doctor can't dispense (403)
- ✅ Pharmacist can't register patient (403)
- ✅ Without token = 401/403
- ✅ Wrong password = 401

---

## 📚 Related Documentation

- **Full Guide:** [ROLE_BASED_TESTING_GUIDE.md](ROLE_BASED_TESTING_GUIDE.md)
- **Original Guide:** [POSTMAN_TESTING_GUIDE.md](POSTMAN_TESTING_GUIDE.md)
- **Visual Guide:** [POSTMAN_VISUAL_GUIDE.md](POSTMAN_VISUAL_GUIDE.md)
- **API Reference:** [DOCS/API_REFERENCE.md](DOCS/API_REFERENCE.md)

---

## 🚀 You're Ready!

Everything is set up. Just:
1. Import the authenticated collection
2. Click "Login as Receptionist" → Send
3. Click any protected endpoint → Send
4. Token is automatically used!

**Happy Testing!** 🎉
