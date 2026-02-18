#!/bin/bash

# Load environment variables from .env
if [ -f .env ]; then
    set -a
    source .env
    set +a
    echo "✓ Environment variables loaded from .env"
else
    echo "✗ Error: .env file not found"
    exit 1
fi

echo ""
echo "═══════════════════════════════════════════════════════════"
echo "Starting Medi-D Spring Boot Application"
echo "═══════════════════════════════════════════════════════════"
echo "Database Host: $DB_HOST"
echo "Database Port: $DB_PORT"
echo "Database Name: $DB_NAME"
echo "Database User: $DB_USER"
echo "Active Profile: $SPRING_PROFILES_ACTIVE"
echo "═══════════════════════════════════════════════════════════"
echo ""

# Run Maven with environment variables exported
mvn spring-boot:run
