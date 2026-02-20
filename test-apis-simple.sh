#!/bin/bash

# Simplified Medi-D API Testing Script

BASE_URL="http://localhost:8080"

echo "===== MEDI-D API TEST REPORT ====="
echo "Date: $(date)"
echo ""

PASSED=0
FAILED=0

# Function to test endpoint
test_api() {
    local method=$1
    local endpoint=$2
    local token=$3
    local expected_code=$4
    local name=$5
    local data=$6
    
    if [ -z "$data" ]; then
        response=$(curl -s -w "|%{http_code}" -X "$method" \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer $token" \
            "$BASE_URL$endpoint")
    else
        response=$(curl -s -w "|%{http_code}" -X "$method" \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer $token" \
            -d "$data" \
            "$BASE_URL$endpoint")
    fi
    
    http_code="${response##*|}"
    body="${response%|*}"
    
    if [[ "$http_code" =~ ^$expected_code ]]; then
        echo "✓ $name - HTTP $http_code"
        ((PASSED++))
        return 0
    else
        echo "✗ $name - Expected $expected_code, got $http_code"
        ((FAILED++))
        return 1
    fi
}

# ============ PUBLIC ENDPOINTS ============
echo "1. PUBLIC ENDPOINTS (No Auth Required)"
test_api "GET" "/api/health" "" "200" "Health Check"
echo ""

# ============ AUTHENTICATION ============
echo "2. AUTHENTICATION"

# Get Doctor Token
doctor_resp=$(curl -s -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"doctor","password":"doctorpass"}')
doctor_token=$(echo "$doctor_resp" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

if [ -z "$doctor_token" ]; then
    echo "✗ Doctor Login Failed"
    ((FAILED++))
else
    echo "✓ Doctor Login Successful"
    ((PASSED++))
fi

# Get Receptionist Token
receptionist_resp=$(curl -s -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"receptionist","password":"receptpass"}')
receptionist_token=$(echo "$receptionist_resp" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

if [ -z "$receptionist_token" ]; then
    echo "✗ Receptionist Login Failed"
    ((FAILED++))
else
    echo "✓ Receptionist Login Successful"
    ((PASSED++))
fi

# Get Pharmacist Token
pharmacist_resp=$(curl -s -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"pharmacist","password":"pharmacistpass"}')
pharmacist_token=$(echo "$pharmacist_resp" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

if [ -z "$pharmacist_token" ]; then
    echo "✗ Pharmacist Login Failed"
    ((FAILED++))
else
    echo "✓ Pharmacist Login Successful"
    ((PASSED++))
fi

echo ""

# ============ PATIENT ENDPOINTS ============
echo "3. PATIENT ENDPOINTS"

# Create Patient
patient_resp=$(curl -s -w "|%{http_code}" -X POST "$BASE_URL/api/patients" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $receptionist_token" \
    -d '{"name":"Test Patient","contact":"+1-555-0100"}')

patient_code="${patient_resp##*|}"
patient_body="${patient_resp%|*}"
patient_id=$(echo "$patient_body" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)

if [[ "$patient_code" =~ ^(200|201) ]]; then
    echo "✓ Create Patient - HTTP $patient_code (ID: $patient_id)"
    ((PASSED++))
else
    echo "✗ Create Patient - HTTP $patient_code"
    ((FAILED++))
    patient_id=1
fi

test_api "GET" "/api/patients/$patient_id" "$receptionist_token" "200" "Get Patient"
echo ""

# ============ APPOINTMENT ENDPOINTS ============
echo "4. APPOINTMENT ENDPOINTS"

# Create Appointment
appt_resp=$(curl -s -w "|%{http_code}" -X POST "$BASE_URL/api/appointments" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $receptionist_token" \
    -d "{\"patientId\":$patient_id,\"doctorId\":10,\"slotTimestamp\":\"2026-03-25T10:00:00\"}")

appt_code="${appt_resp##*|}"
appt_body="${appt_resp%|*}"
appt_id=$(echo "$appt_body" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)

if [[ "$appt_code" =~ ^(200|201) ]]; then
    echo "✓ Create Appointment - HTTP $appt_code (ID: $appt_id)"
    ((PASSED++))
else
    echo "✗ Create Appointment - HTTP $appt_code"
    echo "  Response: $appt_body"
    ((FAILED++))
    appt_id=1
fi

test_api "GET" "/api/appointments/doctor/10" "$receptionist_token" "200" "Get Appointments by Doctor"
test_api "GET" "/api/appointments/patient/$patient_id" "$doctor_token" "200" "Get Patient Appointments (DOCTOR)"
test_api "GET" "/api/appointments/check-slot?doctorId=10&slotTime=2026-03-25T10:00:00" "$receptionist_token" "200" "Check Slot Availability"
echo ""

# ============ MEDICINE ENDPOINTS ============
echo "5. MEDICINE ENDPOINTS"

# Create Medicine
med_resp=$(curl -s -w "|%{http_code}" -X POST "$BASE_URL/api/medicines" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $pharmacist_token" \
    -d '{"name":"Test Medicine","stock":100,"expiryDate":"2027-12-31"}')

med_code="${med_resp##*|}"
med_body="${med_resp%|*}"
med_id=$(echo "$med_body" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)

if [[ "$med_code" =~ ^(200|201) ]]; then
    echo "✓ Create Medicine - HTTP $med_code (ID: $med_id)"
    ((PASSED++))
else
    echo "✗ Create Medicine - HTTP $med_code"
    echo "  Response: $med_body"
    ((FAILED++))
    med_id=1
fi

test_api "GET" "/api/medicines" "$pharmacist_token" "200" "Get All Medicines"
test_api "GET" "/api/medicines/$med_id" "$pharmacist_token" "200" "Get Medicine by ID"
test_api "GET" "/api/medicines/low-stock?minimumStock=50" "$pharmacist_token" "200" "Get Low Stock Medicines"
echo ""

# ============ PRESCRIPTION ENDPOINTS ============
echo "6. PRESCRIPTION ENDPOINTS"

# Create Prescription
if [ -n "$appt_id" ] && [ "$appt_id" != "1" ]; then
    presc_resp=$(curl -s -w "|%{http_code}" -X POST "$BASE_URL/api/prescriptions" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $doctor_token" \
        -d "{\"appointmentId\":$appt_id,\"prescriptionDate\":\"2026-03-25\"}")

    presc_code="${presc_resp##*|}"
    presc_body="${presc_resp%|*}"
    presc_id=$(echo "$presc_body" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)

    if [[ "$presc_code" =~ ^(200|201) ]]; then
        echo "✓ Create Prescription - HTTP $presc_code (ID: $presc_id)"
        ((PASSED++))
    else
        echo "✗ Create Prescription - HTTP $presc_code"
        echo "  Response: $presc_body"
        ((FAILED++))
        presc_id=1
    fi
else
    echo "⊘ Skipping prescription creation (no appointment)"
    presc_id=1
fi

test_api "GET" "/api/prescriptions/pending" "$doctor_token" "200" "Get Pending Prescriptions (DOCTOR)"
test_api "GET" "/api/prescriptions/patient/$patient_id" "$doctor_token" "200" "Get Prescriptions by Patient"
echo ""

# ============ PHARMACY ENDPOINTS ============
echo "7. PHARMACY ENDPOINTS"

test_api "GET" "/api/pharmacy/prescriptions/pending" "$pharmacist_token" "200" "Get Pending (PHARMACIST)"
echo ""

# ============ ROLE-BASED ACCESS CONTROL ============
echo "8. ROLE-BASED ACCESS CONTROL"

# Test Pharmacist cannot access doctor endpoints
response=$(curl -s -w "|%{http_code}" -X GET "$BASE_URL/api/appointments/patient/$patient_id" \
    -H "Authorization: Bearer $pharmacist_token")
code="${response##*|}"
if [[ "$code" == "403" ]]; then
    echo "✓ Pharmacist blocked from doctor endpoint (403)"
    ((PASSED++))
else
    echo "✗ Pharmacist should get 403, got $code"
    ((FAILED++))
fi

# Test Doctor cannot access pharmacy endpoints
response=$(curl -s -w "|%{http_code}" -X GET "$BASE_URL/api/pharmacy/prescriptions/pending" \
    -H "Authorization: Bearer $doctor_token")
code="${response##*|}"
if [[ "$code" == "403" ]]; then
    echo "✓ Doctor blocked from pharmacy endpoint (403)"
    ((PASSED++))
else
    echo "✗ Doctor should get 403, got $code"
    ((FAILED++))
fi

# Test no token is rejected
response=$(curl -s -w "|%{http_code}" -X GET "$BASE_URL/api/patients/$patient_id")
code="${response##*|}"
if [[ "$code" =~ ^(401|403) ]]; then
    echo "✓ Request without token rejected ($code)"
    ((PASSED++))
else
    echo "✗ Should reject no-token request, got $code"
    ((FAILED++))
fi

echo ""
echo "================================"
echo "TOTAL PASSED: $PASSED"
echo "TOTAL FAILED: $FAILED"
echo "SUCCESS RATE: $(echo "scale=1; ($PASSED*100)/($PASSED+$FAILED)" | bc)%"
echo "================================"

if [ $FAILED -eq 0 ]; then
    echo "✓ ALL TESTS PASSED!"
    exit 0
else
    echo "✗ SOME TESTS FAILED - SEE ABOVE"
    exit 1
fi
