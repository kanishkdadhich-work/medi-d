# 🔧 FIXING "FORBIDDEN ACCESS" ERRORS IN POSTMAN

## 🚨 CRITICAL: SecurityConfig Requires JWT Token for ALL Protected Endpoints

**The issue:** Even though many endpoints have NO `@PreAuthorize` annotation, they STILL require a valid JWT Bearer token because of this setting in `SecurityConfig.java`:

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/auth/**").permitAll()      // ← Public
    .requestMatchers("/api/health").permitAll()       // ← Public
    .requestMatchers("/actuator/**").permitAll()      // ← Public
    .requestMatchers("/error").permitAll()            // ← Public
    .anyRequest().authenticated()                     // ← ALL OTHERS NEED JWT!
)
```

---

## ✅ HOW TO FIX "FORBIDDEN ACCESS" IN 5 STEPS

### Step 1: Login to Get Token
```
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "doctor",
  "password": "doctorpass"
}

Response (200 OK):
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJkb2N0b3IiLCJhdXRob3JpdGllcyI6IlJPTEVfRE9DVE9SIiwiaWF0IjoxNjE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
}
```

**Save this token!** You'll need it for every other request.

---

### Step 2: Add Token to EVERY Protected Request

**Find the Authorization Header in Postman:**
```
1. Open any request (e.g., GET /api/patients/1)
2. Click "Headers" tab
3. Add new header:
   Key: Authorization
   Value: Bearer <your_token>
```

**Full Example:**
```
GET http://localhost:8080/api/patients/1
Headers:
  Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

### Step 3: Use Environment Variables (Recommended)

Instead of copy-pasting tokens, use Postman environment variables:

#### Step 3.1: Create Environment
```
1. Postman → Environments → Create New Environment
2. Name: "Medi-D Dev"
3. Variables:
   - receptionist_token: (leave empty)
   - doctor_token: (leave empty)
   - pharmacist_token: (leave empty)
4. Save
```

#### Step 3.2: Set Variables in Login Requests
When you login, Postman AUTOMATICALLY saves tokens to env variables!

```
In the "Tests" tab of login request:

pm.environment.set('doctor_token', pm.response.json().token);
console.log('✅ Doctor token saved');
```

#### Step 3.3: Use Variables in Other Requests
```
GET http://localhost:8080/api/patients/1
Headers:
  Authorization: Bearer {{doctor_token}}
                        ↑ Postman replaces this with actual token
```

---

## 🔍 DEBUGGING: How to Tell WHY You're Getting Forbidden

### Check Response Code & Body

**In Postman Console**, look at response details:

```
Response Code: 403 Forbidden
Response Body: {"error": "Access Denied"}
```

### Issue #1: No Bearer Token at All

**Symptom:**
```
GET /api/patients/1
Headers: (NONE)

Response: 401 or 403
Body: "Unauthorized" or "Full authentication is required"
```

**Fix:**
```
Add Authorization header:
Authorization: Bearer {{doctor_token}}
```

---

### Issue #2: Token is Expired

**Symptom:**
```
GET /api/patients/1
Headers: Authorization: Bearer <old_token>

Response: 403
Body: Token has expired
```

**Fix:**
```
Login again to get fresh token:
POST /api/auth/login
Username: doctor / Password: doctorpass

Copy new token to Authorization header
```

---

### Issue #3: Token is Malformed/Invalid

**Symptom:**
```
GET /api/patients/1
Headers: Authorization: Bearer invalid.fake.token

Response: 403
Body: "Invalid token" or "Unauthorized"
```

**Fix:**
```
Make sure token format is correct:
✅ VALID:   eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJkb2N0b3IiLCJhdXRob3JpdGllcyI6IlJPTEVfRE9DVE9SIn0.Tjva95U...
❌ INVALID: notarealtoken123
❌ INVALID: Token (without Bearer prefix)

CORRECT FORMAT:
Authorization: Bearer <long_base64_string>
```

---

### Issue #4: Wrong Role for Endpoint

**Symptom:**
```
GET /api/pharmacy/prescriptions/pending
Headers: Authorization: Bearer {{doctor_token}}

Response: 403
Body: "Access Denied" or insufficient permissions
```

**Why:** Only PHARMACIST can access `/api/pharmacy/` endpoints

**Fix:**
```
Login as pharmacist instead:
POST /api/auth/login
{
  "username": "pharmacist",
  "password": "pharmacistpass"
}

Use pharmacist_token in Authorization header
```

---

### Issue #5: Bearer Format Typo

**Symptom:**
```
❌ Authorization: Bearereyj... (no space)
❌ Authorization: Token eyj... (wrong prefix)
❌ Authorization: eyj... (missing Bearer)
❌ Authorizaton: Bearer eyj... (typo in header name)
```

**Fix:**
```
✅ CORRECT format:
Authorization: Bearer <token>
              ↑space↑
```

---

## 📋 QUICK CHECKLIST: Before Sending Request

- [ ] Is app running? (`./mvnw spring-boot:run`)
- [ ] Did I login? (POST /api/auth/login)
- [ ] Do I have a token? (Check environment variable or response)
- [ ] Is token in Authorization header? (Bearer format)
- [ ] Did I spell "Authorization" correctly?
- [ ] Do I have space between "Bearer" and token?
- [ ] Is the endpoint protected? (Requires auth)
- [ ] Do I have the right role? (Check endpoint requirements)

---

## 🧪 TEST EACH STEP

### Test 1: Public Endpoint (No Auth)
```
GET /api/health
Headers: (NONE)

Expected: 200 OK
Body: "Medi-D System is up and running!"
```

### Test 2: Login Works
```
POST /api/auth/login
Body: {"username": "doctor", "password": "doctorpass"}

Expected: 200 OK
Body: {"token": "eyJ..."}
```

### Test 3: Protected Endpoint WITH Token
```
GET /api/patients/1
Headers: Authorization: Bearer {{doctor_token}}

Expected: 200 OK  (or 404 if patient doesn't exist)
```

### Test 4: Protected Endpoint WITHOUT Token
```
GET /api/patients/1
Headers: (NONE)

Expected: 401 or 403 (should fail)
```

### Test 5: Wrong Role for Endpoint
```
GET /api/pharmacy/prescriptions/pending
Headers: Authorization: Bearer {{doctor_token}} (doctor, not pharmacist!)

Expected: 403 Forbidden
```

### Test 6: Correct Role for Endpoint
```
GET /api/pharmacy/prescriptions/pending
Headers: Authorization: Bearer {{pharmacist_token}}

Expected: 200 OK (empty list if no prescriptions)
```

---

## 🎯 POSTMAN SETUP GUIDE

### Option A: Manual Token Copy-Paste (Simple)

1. **Login Request:**
   ```
   POST /api/auth/login
   Body: {"username": "doctor", "password": "doctorpass"}
   ```

2. **Copy Token:**
   - Send request
   - Look at response: `{"token": "eyJ..."}`
   - Copy the long string

3. **Add to Header:**
   ```
   In ANY request:
   Authorization: Bearer eyJ...  (paste token here)
   ```

---

### Option B: Automatic Token Saving (Recommended)

1. **Create Login Request:**
   ```
   POST /api/auth/login
   Body: {"username": "doctor", "password": "doctorpass"}
   ```

2. **Add Test Script:**
   ```
   Click "Tests" tab and paste this:
   
   if (pm.response.code === 200) {
       pm.environment.set('doctor_token', pm.response.json().token);
       console.log('✅ Token saved!');
   }
   ```

3. **Send Request:**
   - Click Send
   - Token automatically saves to environment variable `doctor_token`

4. **Use in Other Requests:**
   ```
   Authorization: Bearer {{doctor_token}}
   ```

---

## 🛠️ POSTMAN ENVIRONMENT SETUP

### Create New Environment
```
1. Postman → Environments (left sidebar) → Create New
2. Name: Medi-D Development
3. Variables:
```

| Variable | Initial Value | Description |
|----------|---------------|-------------|
| receptionist_token | | Auto-filled after receptionist login |
| doctor_token | | Auto-filled after doctor login |
| pharmacist_token | | Auto-filled after pharmacist login |
| patient_id | 1 | Update after creating patient |
| appointment_id | | Update after creating appointment |
| prescription_id | | Update after creating prescription |

### Select Environment
```
1. Top right of Postman, where it says "No Environment"
2. Click dropdown
3. Select "Medi-D Development"
```

---

## 🔄 COMPLETE WORKFLOW: Zero to Testing

```
STEP 1: Start App
$ ./mvnw spring-boot:run
Wait for: "Started MediDApplication in X.XXX seconds"

STEP 2: Import Collection
Postman → File → Import → medi-d-postman-collection-FIXED.json

STEP 3: Select Environment
Top right → Select "Medi-D Development"

STEP 4: Login as Doctor
Folder: "🔐 AUTHENTICATION"
Click: "2️⃣ Login as DOCTOR"
Click: Send
Wait for: "✅ Doctor token saved" in console

STEP 5: Test Protected Endpoint
Folder: "👨‍⚕️ DOCTOR ENDPOINTS"
Click: "Get Appointments by Patient"
Click: Send
Expected: 200 OK

STEP 6: Test Role Enforcement
Folder: "🔒 SECURITY & ROLE TESTS"
Click: "❌ Test: Doctor Cannot Access Pharmacy"
Click: Send
Expected: 403 Forbidden (GOOD!)

STEP 7: Switch Roles
Go back to "🔐 AUTHENTICATION"
Click: "3️⃣ Login as PHARMACIST"
Click: Send
Wait for: "✅ Pharmacist token saved"

STEP 8: Test Pharmacist Endpoint
Folder: "💊 PHARMACIST ENDPOINTS"
Click: "Get Pending Prescriptions (PHARMACIST ONLY)"
Click: Send
Expected: 200 OK (empty list is OK)
```

---

## 🆘 STILL GETTING 403? FINAL CHECKLIST

1. **Database Connected?**
   ```
   $ ./test-db-connection.sh
   Should show: "Connection successful"
   ```

2. **App Running?**
   ```
   Terminal output should show:
   "Started MediDApplication in X.XXX seconds"
   ```

3. **Token Actually Saved?**
   ```
   Postman → Environments → Medi-D Development
   Check: does doctor_token have a value?
   ```

4. **Bearer Format Correct?**
   ```
   Authorization: Bearer eyJhbGc...
   NOT: Authorization: eyJhbGc...
   NOT: Authorization: Token eyJhbGc...
   ```

5. **Right Endpoint for Role?**
   ```
   PHARMACIST endpoints: /api/pharmacy/...
   DOCTOR endpoints: /api/appointments/patient/... AND /api/prescriptions/...
   RECEPTIONIST: /api/appointments (create/read), /api/patients (create/read)
   ```

6. **Check Response Body**
   ```
   In Postman: Click "Response Body" tab
   Look for error message details
   ```

---

## 📞 COMMON RESPONSES & MEANINGS

| Code | Message | Meaning | Fix |
|------|---------|---------|-----|
| 401 | Unauthorized | No token or invalid token | Add Bearer token to header |
| 403 | Forbidden | Wrong role for endpoint | Use correct role's token |
| 404 | Not Found | Resource doesn't exist | Create resource first or check ID |
| 400 | Bad Request | Invalid request body | Check JSON format and required fields |
| 500 | Server Error | Backend problem | Check server logs, restart app |
| 200 | OK | Success | All good! ✅ |
| 201 | Created | Resource created | Success, check response ID ✅ |

