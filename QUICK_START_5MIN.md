# ⚡ QUICK START: Fix "Forbidden" & Start Testing (5 Minutes)

## 🎯 ONE LINE PROBLEM STATEMENT
Your Postman requests were getting 403 Forbidden because:
- **SecurityConfig requires JWT Bearer token for ALL protected endpoints**
- Your requests didn't have the Bearer token header
- OR the token was invalid/expired

**Solution:** Add `Authorization: Bearer <token>` header to every protected request

---

## ✅ 5-MINUTE SETUP

### Minute 1: Start Your App
```bash
# Terminal 1
cd /home/kanishk/IdeaProjects/medi-d
./mvnw spring-boot:run

# Wait for:
# "Started MediDApplication in X.XXX seconds"
```

### Minute 2: Verify Database
```bash
# Terminal 2
cd /home/kanishk/IdeaProjects/medi-d
./test-db-connection.sh

# Should show: "Connection successful"
```

### Minute 3: Import Postman Collection
```
Postman Menu:
1. File → Import
2. Select: medi-d-postman-collection-FIXED.json
3. Click Import
```

### Minute 4: Create Environment
```
Postman Menu:
1. Environments → Create New Environment
2. Name: "Medi-D Dev"
3. Save

Or use existing environment if you have one
```

### Minute 5: Login & Test
```
In Postman:
1. Folder: "🔐 AUTHENTICATION"
2. Click: "2️⃣ Login as DOCTOR"
3. Click: "Send"
4. Wait for console: "✅ Doctor token saved"
5. Now click ANY other request
6. It will automatically use {{doctor_token}} in Authorization header!
7. Click: "Send"
8. Expected: 200 OK (not 403!)
```

---

## 🔑 Pre-configured Test Users

```
╔════════════════════════════════════════════════╗
║ Username:    doctor                            ║
║ Password:    doctorpass                        ║
║ Role:        DOCTOR                            ║
╠════════════════════════════════════════════════╣
║ Username:    receptionist                      ║
║ Password:    receptpass                        ║
║ Role:        RECEPTIONIST                      ║
╠════════════════════════════════════════════════╣
║ Username:    pharmacist                        ║
║ Password:    pharmacistpass                    ║
║ Role:        PHARMACIST                        ║
╚════════════════════════════════════════════════╝
```

---

## 🎯 WHAT WILL HAPPEN

### ✅ Working requests (200 OK)
```
GET /api/health                                  NO AUTH NEEDED
POST /api/auth/login (any credentials)           NO AUTH NEEDED
GET /api/patients/1 + Bearer token              200 OK
POST /api/appointments + Bearer token            201 CREATED
GET /api/prescriptions/pending + doctor token   200 OK
GET /api/pharmacy/prescriptions/pending + pharmacist token  200 OK
```

### ❌ Failing requests (403 Forbidden) - THIS IS CORRECT!
```
GET /api/patients/1 (NO Bearer token)           403 FORBIDDEN
GET /api/pharmacy/prescriptions/pending + doctor token  403 FORBIDDEN
GET /api/appointments/mark-unavailable + receptionist  403 FORBIDDEN
```

**These failures are SECURITY WORKING!** 🔒

---

## 📋 UNDERSTANDING 403 vs 401 vs 200

| Code | Meaning | Your Action |
|------|---------|-------------|
| **200 OK** | Success! Request worked | ✅ Everything good |
| **201 CREATED** | Resource created | ✅ Everything good |
| **400 Bad Request** | Invalid request body | ❌ Check JSON format |
| **401 Unauthorized** | No/invalid token | ❌ Login again for fresh token |
| **403 Forbidden** | Token valid but wrong role | ❌ Use correct role's token |
| **404 Not Found** | Resource doesn't exist | ❌ Create it first or check ID |
| **500 Server Error** | App crashed | ❌ Check app logs, restart |

---

## 🔍 IF YOU GET 403 FORBIDDEN

**Quick Debug:**

1. **Check Postman Console:**
   ```
   At bottom left → Click "Console"
   Look for error message from server
   ```

2. **Verify Bearer Token:**
   ```
   In request → Headers tab
   Should show: Authorization: Bearer eyJhbGc...
   NOT just: eyJhbGc...
   ```

3. **Verify Environment Variable:**
   ```
   Postman → Environments → Medi-D Dev
   Check: doctor_token has a value (not empty)
   ```

4. **Re-login:**
   ```
   Folder: "🔐 AUTHENTICATION"
   Click: "2️⃣ Login as DOCTOR"
   Send
   Wait for: "✅ Doctor token saved"
   ```

5. **Check Right Endpoint for Role:**
   ```
   /api/pharmacy/prescriptions/pending  →  PHARMACIST token required
   /api/appointments/mark-unavailable   →  DOCTOR token required
   /api/appointments/patient/{id}       →  DOCTOR or RECEPTIONIST token
   ```

---

## 🧪 TEST YOUR SETUP (The Workflow)

```
STEP 1: Health Check (Public, no token)
├─ GET /api/health
└─ Expected: "Medi-D System is up and running!"

STEP 2: Login (Public, no token)
├─ POST /api/auth/login
├─ Body: {"username": "doctor", "password": "doctorpass"}
└─ Expected: {"token": "eyJhbGc..."} (long string)

STEP 3: Use Token (Protected, needs Bearer)
├─ GET /api/patients/1
├─ Header: Authorization: Bearer <token from step 2>
└─ Expected: {"id": 1, "name": "John Smith"} or NOT_FOUND (404)

STEP 4: Test Role Security (Protected, wrong role)
├─ GET /api/pharmacy/prescriptions/pending
├─ Header: Authorization: Bearer <doctor_token> (wrong role!)
└─ Expected: 403 Forbidden (GOOD SECURITY!)

STEP 5: Test Correct Role (Protected, right role)
├─ GET /api/pharmacy/prescriptions/pending
├─ Header: Authorization: Bearer <pharmacist_token> (right role)
└─ Expected: 200 OK [] (empty list is fine)
```

---

## 📁 FILES YOU NEED

```
✅ MUST READ FIRST:
├─ INVESTIGATION_SUMMARY.md        (2 min read - overview)
├─ API_SECURITY_AUDIT.md           (5 min read - what endpoints need what)
└─ API_ENDPOINT_REFERENCE.md       (reference table)

✅ MUST USE:
└─ medi-d-postman-collection-FIXED.json  (Import this into Postman)

✅ USE IF STUCK:
└─ POSTMAN_TROUBLESHOOTING_GUIDE.md  (Debug 403 errors)

📁 LOCATION: /home/kanishk/IdeaProjects/medi-d/
```

---

## ⏱️ TIMELINE: What Happens Next

```
T+0 min:   You start app (./mvnw spring-boot:run)
T+30 sec:  App connects to database
T+1 min:   App ready, listening on :8080
T+2 min:   You import Postman collection
T+3 min:   You create environment
T+4 min:   You login as doctor (POST /api/auth/login)
T+4:30:    Token received, auto-saved to {{doctor_token}} variable
T+5 min:   You test any endpoint
           - If it has Bearer token in header → 200 OK ✅
           - If it doesn't have token → 403 Forbidden (correct!) ✅
```

---

## 🎯 YOUR ROLES (What Each Can Do)

### RECEPTIONIST (receptionist / receptpass)
```
✅ Register patients               POST /api/patients
✅ View patient details           GET /api/patients/{id}
✅ Book appointments              POST /api/appointments
✅ View patient appointments      GET /api/appointments/patient/{id}
✅ View doctor schedules          GET /api/appointments/doctor/{id}
✅ Check slot availability        GET /api/appointments/check-slot
❌ Create prescriptions (can't)   POST /api/prescriptions fails

Expected when testing:
- Your endpoints work (200 OK)
- /api/pharmacy endpoints give 403 (forbidden)
- /api/appointments/mark-unavailable gives 403 (only doctors)
```

### DOCTOR (doctor / doctorpass)
```
✅ View patient appointments      GET /api/appointments/patient/{id}
✅ View pending prescriptions     GET /api/prescriptions/pending
✅ Create prescriptions           POST /api/prescriptions
✅ Mark slots unavailable         POST /api/appointments/mark-unavailable
✅ View medicines                 GET /api/medicines

Expected when testing:
- Your endpoints work (200 OK)
- /api/pharmacy endpoints give 403 (only pharmacists)
- You can't dispense from /api/pharmacy (use /api/prescriptions/)
```

### PHARMACIST (pharmacist / pharmacistpass)
```
✅ View pending prescriptions     GET /api/pharmacy/prescriptions/pending
✅ Dispense medications           POST /api/pharmacy/prescriptions/{id}/dispense
✅ View medicines                 GET /api/medicines
✅ Create/update medicines        POST/PUT /api/medicines
✅ Check low stock                GET /api/medicines/low-stock

Expected when testing:
- Your pharmacy endpoints work (200 OK)
- /api/appointments endpoints work (read-only)
- /api/appointments/patient/{id} gives 403 (doctor/receptionist only)
- /api/prescriptions endpoints give 403 (not a doctor/receptionist)
```

---

## ✨ PRO TIPS

1. **Use Postman Environment Variables**
   - After login, tokens auto-save to `{{doctor_token}}`
   - Just use `Authorization: Bearer {{doctor_token}}` in header
   - Postman auto-replaces with actual value

2. **Test Scripts Verify Success**
   - Each request has a test script
   - Green checkmark = test passed
   - Red X = test failed

3. **Console Shows Details**
   - Postman → Console (bottom left)
   - See exactly what's happening
   - See error messages from server

4. **Postman Collections are Reusable**
   - Can share with team
   - Team imports same collection
   - Everyone has same test setup

---

## 🚀 YOU'RE READY!

Everything you need is set up:
- ✅ SecurityConfig verified
- ✅ All 31+ endpoints documented
- ✅ 5 issues identified & explained
- ✅ Postman collection ready
- ✅ Troubleshooting guide prepared
- ✅ Test users configured
- ✅ Role matrix documented

**Just follow the 5-minute setup above and you'll be testing!** 🎉

---

## 🆘 FINAL CHECKLIST BEFORE TESTING

- [ ] App is running (`./mvnw spring-boot:run`)
- [ ] Database is working (`./test-db-connection.sh`)
- [ ] Postman collection imported (medi-d-postman-collection-FIXED.json)
- [ ] Environment created ("Medi-D Dev")
- [ ] You understand: 403 = wrong role/no token (GOOD SECURITY!)
- [ ] You understand: 200 = success, request worked
- [ ] You know test users (receptionist/receptpass, etc.)
- [ ] You know Bearer token format: `Authorization: Bearer <token>`

If all checked ✅ → Go test! 

If any ❌ → Read POSTMAN_TROUBLESHOOTING_GUIDE.md

