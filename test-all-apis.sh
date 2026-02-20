#!/bin/bash

# Medi-D API Testing Script
# This script tests all endpoints and verifies authentication/authorization

set -e

BASE_URL="http://localhost:8080"
RESULTS_FILE="API_TEST_RESULTS.txt"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Initialize results file
echo "===== MEDI-D API TESTING REPORT =====" > "$RESULTS_FILE"
echo "Date: $(date)" >> "$RESULTS_FILE"
echo "" >> "$RESULTS_FILE"

# Test counter
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Function to test an endpoint
test_endpoint() {
    local method=$1
    local endpoint=$2
    local token=$3
    local expected_code=$4
    local description=$5
    local data=$6
    
    ((TOTAL_TESTS++)) || true
    
    echo -n "Testing $endpoint... "
    
    if [ -z "$data" ]; then
        response=$(curl -s -w "\n%{http_code}" -X "$method" \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer $token" \
            "$BASE_URL$endpoint")
    else
        response=$(curl -s -w "\n%{http_code}" -X "$method" \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer $token" \
            -d "$data" \
            "$BASE_URL$endpoint")
    fi
    
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    
    if [[ "$http_code" == "$expected_code"* ]]; then
        echo -e "${GREEN}✓ PASS${NC} (HTTP $http_code)"
        echo "✓ $description - HTTP $http_code" >> "$RESULTS_FILE"
        ((PASSED_TESTS++)) || true
        echo "$body"
        return 0
    else
        echo -e "${RED}✗ FAIL${NC} (Expected $expected_code, got $http_code)"
        echo "✗ $description - Expected $expected_code, got $http_code" >> "$RESULTS_FILE"
        echo "  Response: $body" >> "$RESULTS_FILE"
        ((FAILED_TESTS++)) || true
        return 1
    fi
}

# Function to extract token from response
extract_token() {
    echo "$1" | grep -o '"token":"[^"]*"' | cut -d'"' -f4
}

# Function to extract ID from response
extract_field() {
    echo "$1" | grep -o "\"$2\":\"[^\"]*\"" | cut -d'"' -f4 | head -1
}

echo -e "${BLUE}=== MEDI-D API COMPREHENSIVE TEST SUITE ===${NC}\n"

# ============================================
# TEST 1: PUBLIC ENDPOINTS (NO AUTH REQUIRED)
# ============================================
echo -e "${BLUE}1. Testing PUBLIC Endpoints (No Auth)${NC}"

test_endpoint "GET" "/api/health" "" "200" "Health Check" ""

echo ""

# ============================================
# TEST 2: AUTHENTICATION
# ============================================
echo -e "${BLUE}2. Testing AUTHENTICATION${NC}"

# Login as Doctor
echo "Logging in as DOCTOR..."
doctor_response=$(curl -s -X POST \
    -H "Content-Type: application/json" \
    -d '{"username":"doctor","password":"doctorpass"}' \
    "$BASE_URL/api/auth/login")

doctor_token=$(extract_token "$doctor_response")
if [ -z "$doctor_token" ]; then
    echo -e "${RED}✗ Failed to get DOCTOR token${NC}"
    echo "Response: $doctor_response" >> "$RESULTS_FILE"
    exit 1
fi
echo -e "${GREEN}✓ Got DOCTOR token${NC}"
echo "DOCTOR Token: $doctor_token" >> "$RESULTS_FILE"

# Login as Receptionist
echo "Logging in as RECEPTIONIST..."
receptionist_response=$(curl -s -X POST \
    -H "Content-Type: application/json" \
    -d '{"username":"receptionist","password":"receptpass"}' \
    "$BASE_URL/api/auth/login")

receptionist_token=$(extract_token "$receptionist_response")
if [ -z "$receptionist_token" ]; then
    echo -e "${RED}✗ Failed to get RECEPTIONIST token${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Got RECEPTIONIST token${NC}"
echo "RECEPTIONIST Token: $receptionist_token" >> "$RESULTS_FILE"

# Login as Pharmacist
echo "Logging in as PHARMACIST..."
pharmacist_response=$(curl -s -X POST \
    -H "Content-Type: application/json" \
    -d '{"username":"pharmacist","password":"pharmacistpass"}' \
    "$BASE_URL/api/auth/login")

pharmacist_token=$(extract_token "$pharmacist_response")
if [ -z "$pharmacist_token" ]; then
    echo -e "${RED}✗ Failed to get PHARMACIST token${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Got PHARMACIST token${NC}"
echo "PHARMACIST Token: $pharmacist_token" >> "$RESULTS_FILE"

echo ""

# ============================================
# TEST 3: PATIENT ENDPOINTS
# ============================================
echo -e "${BLUE}3. Testing PATIENT Endpoints${NC}"

# Create patient
patient_data='{"name":"John Smith","contact":"+1-555-0100"}'
patient_response=$(curl -s -w "\n%{http_code}" -X POST \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $receptionist_token" \
    -d "$patient_data" \
    "$BASE_URL/api/patients")

patient_http=$(echo "$patient_response" | tail -n1)
patient_body=$(echo "$patient_response" | sed '$d')

if [[ "$patient_http" == "201" ]] || [[ "$patient_http" == "200" ]]; then
    echo -e "${GREEN}✓ Created patient${NC}"
    ((PASSED_TESTS++)) || true
    patient_id=$(echo "$patient_body" | grep -o '"patient_id":[0-9]*' | head -1 | cut -d':' -f2)
    if [ -z "$patient_id" ]; then
        patient_id=$(echo "$patient_body" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
    fi
    echo "Patient ID: $patient_id" >> "$RESULTS_FILE"
else
    echo -e "${RED}✗ Failed to create patient (HTTP $patient_http)${NC}"
    ((FAILED_TESTS++)) || true
    patient_id="1"  # Fallback ID
fi
((TOTAL_TESTS++)) || true

# Get patient
test_endpoint "GET" "/api/patients/$patient_id" "$receptionist_token" "200" "Get Patient" "" 

echo ""

# ============================================
# TEST 4: APPOINTMENT ENDPOINTS
# ============================================
echo -e "${BLUE}4. Testing APPOINTMENT Endpoints${NC}"

# Create appointment
appointment_data='{"patientId":'$patient_id',"doctorId":10,"slotTimestamp":"2026-03-25T10:00:00","status":"SCHEDULED"}'
appointment_response=$(curl -s -w "\n%{http_code}" -X POST \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $receptionist_token" \
    -d "$appointment_data" \
    "$BASE_URL/api/appointments")

appointment_http=$(echo "$appointment_response" | tail -n1)
appointment_body=$(echo "$appointment_response" | sed '$d')

if [[ "$appointment_http" == "201" ]] || [[ "$appointment_http" == "200" ]]; then
    echo -e "${GREEN}✓ Created appointment${NC}"
    ((PASSED_TESTS++)) || true
    appointment_id=$(echo "$appointment_body" | grep -o '"appointment_id":[0-9]*' | head -1 | cut -d':' -f2)
    if [ -z "$appointment_id" ]; then
        appointment_id=$(echo "$appointment_body" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
    fi
    echo "Appointment ID: $appointment_id" >> "$RESULTS_FILE"
else
    echo -e "${RED}✗ Failed to create appointment (HTTP $appointment_http)${NC}"
    echo "Response: $appointment_body" >> "$RESULTS_FILE"
    ((FAILED_TESTS++)) || true
    appointment_id="1"  # Fallback
fi
((TOTAL_TESTS++)) || true

# Get appointment by doctor
test_endpoint "GET" "/api/appointments/doctor/10" "$receptionist_token" "200" "Get Appointments by Doctor" ""

# Get appointment by patient (requires DOCTOR or RECEPTIONIST)
test_endpoint "GET" "/api/appointments/patient/$patient_id" "$doctor_token" "200" "Get Appointments by Patient (DOCTOR)" ""

# Check slot availability
test_endpoint "GET" "/api/appointments/check-slot?doctorId=10&slotTime=2026-03-25T14:00:00" "$receptionist_token" "200" "Check Slot Availability" ""

echo ""

# ============================================
# TEST 5: MEDICINE ENDPOINTS
# ============================================
echo -e "${BLUE}5. Testing MEDICINE Endpoints${NC}"

# Create medicine
medicine_data='{"name":"Aspirin","stock":100,"expiryDate":"2027-12-31"}'
medicine_response=$(curl -s -w "\n%{http_code}" -X POST \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $pharmacist_token" \
    -d "$medicine_data" \
    "$BASE_URL/api/medicines")

medicine_http=$(echo "$medicine_response" | tail -n1)
medicine_body=$(echo "$medicine_response" | sed '$d')

if [[ "$medicine_http" == "201" ]] || [[ "$medicine_http" == "200" ]]; then
    echo -e "${GREEN}✓ Created medicine${NC}"
    ((PASSED_TESTS++)) || true
    medicine_id=$(echo "$medicine_body" | grep -o '"medicine_id":[0-9]*' | head -1 | cut -d':' -f2)
    if [ -z "$medicine_id" ]; then
        medicine_id=$(echo "$medicine_body" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
    fi
    echo "Medicine ID: $medicine_id" >> "$RESULTS_FILE"
else
    echo -e "${RED}✗ Failed to create medicine (HTTP $medicine_http)${NC}"
    ((FAILED_TESTS++)) || true
    medicine_id="1"  # Fallback
fi
((TOTAL_TESTS++)) || true

# Get all medicines
test_endpoint "GET" "/api/medicines" "$pharmacist_token" "200" "Get All Medicines" ""

# Get medicine by ID
test_endpoint "GET" "/api/medicines/$medicine_id" "$pharmacist_token" "200" "Get Medicine by ID" ""

# Low stock medicines
test_endpoint "GET" "/api/medicines/low-stock?minimumStock=50" "$pharmacist_token" "200" "Get Low Stock Medicines" ""

echo ""

# ============================================
# TEST 6: PRESCRIPTION ENDPOINTS
# ============================================
echo -e "${BLUE}6. Testing PRESCRIPTION Endpoints${NC}"

if [ -n "$appointment_id" ] && [ "$appointment_id" != "1" ]; then
    # Create prescription
    prescription_data='{"appointmentId":'$appointment_id',"diagnosis":"General consultation","status":"PENDING"}'
    prescription_response=$(curl -s -w "\n%{http_code}" -X POST \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $doctor_token" \
        -d "$prescription_data" \
        "$BASE_URL/api/prescriptions")

    prescription_http=$(echo "$prescription_response" | tail -n1)
    prescription_body=$(echo "$prescription_response" | sed '$d')

    if [[ "$prescription_http" == "201" ]] || [[ "$prescription_http" == "200" ]]; then
        echo -e "${GREEN}✓ Created prescription${NC}"
        ((PASSED_TESTS++)) || true
        prescription_id=$(echo "$prescription_body" | grep -o '"prescription_id":[0-9]*' | head -1 | cut -d':' -f2)
        if [ -z "$prescription_id" ]; then
            prescription_id=$(echo "$prescription_body" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
        fi
        echo "Prescription ID: $prescription_id" >> "$RESULTS_FILE"
    else
        echo -e "${RED}✗ Failed to create prescription (HTTP $prescription_http)${NC}"
        echo "Response: $prescription_body" >> "$RESULTS_FILE"
        ((FAILED_TESTS++)) || true
        prescription_id="1"
    fi
    ((TOTAL_TESTS++)) || true
else
    echo -e "${YELLOW}⊘ Skipping prescription tests (no appointment ID)${NC}"
    prescription_id="1"
fi

# Get pending prescriptions
test_endpoint "GET" "/api/prescriptions/pending" "$doctor_token" "200" "Get Pending Prescriptions (DOCTOR)" ""

# Get prescriptions by patient
test_endpoint "GET" "/api/prescriptions/patient/$patient_id" "$doctor_token" "200" "Get Prescriptions by Patient" ""

echo ""

# ============================================
# TEST 7: PHARMACY ENDPOINTS
# ============================================
echo -e "${BLUE}7. Testing PHARMACY Endpoints (PHARMACIST ONLY)${NC}"

# Get pending prescriptions (PHARMACIST only)
test_endpoint "GET" "/api/pharmacy/prescriptions/pending" "$pharmacist_token" "200" "Get Pending Prescriptions (PHARMACIST)" ""

echo ""

# ============================================
# TEST 8: ROLE-BASED ACCESS CONTROL
# ============================================
echo -e "${BLUE}8. Testing ROLE-BASED ACCESS CONTROL${NC}"

echo "Testing Pharmacist CANNOT access Doctor endpoints..."
test_endpoint "GET" "/api/appointments/patient/$patient_id" "$pharmacist_token" "403" "Pharmacy: Cannot access patient appointments" "" > /dev/null 2>&1 || true

echo "Testing Doctor CANNOT access Pharmacy endpoints..."
test_endpoint "GET" "/api/pharmacy/prescriptions/pending" "$doctor_token" "403" "Doctor: Cannot access pharmacy endpoints" "" > /dev/null 2>&1 || true

echo "Testing endpoints WITHOUT token are forbidden..."
response=$(curl -s -w "\n%{http_code}" -X GET \
    -H "Content-Type: application/json" \
    "$BASE_URL/api/patients/1")

http_code=$(echo "$response" | tail -n1)
if [[ "$http_code" == "401" ]] || [[ "$http_code" == "403" ]]; then
    echo -e "${GREEN}✓ Correctly rejected request without token (HTTP $http_code)${NC}"
    ((PASSED_TESTS++)) || true
else
    echo -e "${RED}✗ Should reject without token, but got HTTP $http_code${NC}"
    ((FAILED_TESTS++)) || true
fi
((TOTAL_TESTS++)) || true

echo ""

# ============================================
# FINAL REPORT
# ============================================
echo -e "${BLUE}=== TEST SUMMARY ===${NC}"
echo "Total Tests: $TOTAL_TESTS"
echo -e "Passed: ${GREEN}$PASSED_TESTS${NC}"
echo -e "Failed: ${RED}$FAILED_TESTS${NC}"

echo "" >> "$RESULTS_FILE"
echo "=== FINAL SUMMARY ===" >> "$RESULTS_FILE"
echo "Total Tests: $TOTAL_TESTS" >> "$RESULTS_FILE"
echo "Passed: $PASSED_TESTS" >> "$RESULTS_FILE"
echo "Failed: $FAILED_TESTS" >> "$RESULTS_FILE"
echo "Success Rate: $(echo "scale=2; ($PASSED_TESTS * 100) / $TOTAL_TESTS" | bc)%" >> "$RESULTS_FILE"

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "\n${GREEN}✓ ALL TESTS PASSED!${NC}\n"
    echo "Status: ALL TESTS PASSED" >> "$RESULTS_FILE"
    exit 0
else
    echo -e "\n${RED}✗ SOME TESTS FAILED${NC}\n"
    echo "Status: SOME TESTS FAILED" >> "$RESULTS_FILE"
    exit 1
fi
