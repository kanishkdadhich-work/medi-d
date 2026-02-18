#!/bin/bash

# Medi-D PostgreSQL Database Setup Script

# Load environment variables from .env file
if [ -f .env ]; then
    set -a
    source .env
    set +a
else
    echo "Error: .env file not found!"
    echo "Please create a .env file in the project root directory."
    exit 1
fi

# Database configuration (from .env)
DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-5432}
DB_USER=${DB_USER:-postgres}
DB_PASSWORD=${DB_PASSWORD:-postgres}
DB_NAME=${DB_NAME:-medid_db}

echo "═══════════════════════════════════════════════════════════"
echo "Medi-D PostgreSQL Database Setup"
echo "═══════════════════════════════════════════════════════════"
echo "Database Host: $DB_HOST"
echo "Database Port: $DB_PORT"
echo "Database User: $DB_USER"
echo "Database Name: $DB_NAME"
echo "═══════════════════════════════════════════════════════════"

# Export password for psql
export PGPASSWORD=$DB_PASSWORD

# Create database
echo "Creating database '$DB_NAME'..."
psql -h $DB_HOST -U $DB_USER -p $DB_PORT -c "CREATE DATABASE $DB_NAME;" 2>/dev/null

if [ $? -eq 0 ] || [ $? -eq 1 ]; then
    echo "✓ Database ready (created or already exists)"
else
    echo "✗ Failed to create database"
    exit 1
fi

# Create table
echo "Creating patients table..."
psql -h $DB_HOST -U $DB_USER -p $DB_PORT -d $DB_NAME -f src/main/resources/db/migration/V1__Create_patients_table.sql

if [ $? -eq 0 ]; then
    echo "✓ Tables created successfully"
else
    echo "✗ Failed to create tables"
    exit 1
fi

echo ""
echo "═══════════════════════════════════════════════════════════"
echo "✓ Database setup completed!"
echo "═══════════════════════════════════════════════════════════"
echo ""
echo "You can now run your Spring Boot application using:"
echo "  ./run-app.sh"
echo ""
echo "Or use Maven directly:"
echo "  source .env && mvn spring-boot:run"
