#!/bin/bash

echo "========== MEDI-D AUTHENTICATION DEBUG =========="
echo ""
echo "1. Testing health endpoint (public, no auth needed):"
curl -s http://localhost:8080/api/health
echo ""
echo ""

echo "2. Testing login and extracting token:"
LOGIN_RESPONSE=$(curl -s -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"doctor","password":"doctorpass"}')

echo "Login Response:"
echo "$LOGIN_RESPONSE" | jq . 2>/dev/null || echo "$LOGIN_RESPONSE"

TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
echo ""
echo "Extracted Token (first 50 chars): ${TOKEN:0:50}..."
echo "Token Length: ${#TOKEN}"
echo ""

echo "3. Testing protected endpoint WITHOUT token:"
curl -s -w "\nHTTP Code: %{http_code}\n" -X GET "http://localhost:8080/api/patients/1" | head -5
echo ""

echo "4. Testing protected endpoint WITH token in Authorization header:"
echo "Authorization Header: Bearer ${TOKEN:0:50}..."
curl -s -w "\nHTTP Code: %{http_code}\n" -X GET "http://localhost:8080/api/patients/1" \
  -H "Authorization: Bearer $TOKEN" 2>&1 | head -20
echo ""

echo "5. Checking if token is being sent correctly:"
curl -s -i -X GET "http://localhost:8080/api/patients/1" \
  -H "Authorization: Bearer $TOKEN" 2>&1 | head -15
echo ""

echo "========== END DEBUG ==========="
