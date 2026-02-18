#!/bin/bash

# Medi-D PostgreSQL Database Setup Script

echo "═══════════════════════════════════════════════════════════"
echo "Medi-D PostgreSQL Database Setup"
echo "═══════════════════════════════════════════════════════════"

# Load environment variables from .env file
if [ -f .env ]; then
    set -a
    source .env
    set +a
    echo "✓ Environment variables loaded from .env"
else
    echo "✗ Error: .env file not found!"
    exit 1
fi

echo "Database Host: $DB_HOST"
echo "Database Port: $DB_PORT"
echo "Database Name: $DB_NAME"
echo "Database User: $DB_USER"
echo "═══════════════════════════════════════════════════════════"
echo ""

# Note: Database and user should be created beforehand using:
# sudo -u postgres psql << 'EOF'
# CREATE DATABASE medid_test;
# CREATE USER medid_admin WITH PASSWORD 'medid_pass';
# GRANT ALL PRIVILEGES ON DATABASE medid_test TO medid_admin;
# \c medid_test
# GRANT ALL ON SCHEMA public TO medid_admin;
# ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO medid_admin;
# ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO medid_admin;
# EOF

echo "Testing database connection..."
export PGPASSWORD=$DB_PASSWORD
timeout 5 psql -h $DB_HOST -U $DB_USER -p $DB_PORT -d $DB_NAME -c "SELECT 1;" >/dev/null 2>&1

if [ $? -eq 0 ]; then
    echo "✓ Database connection successful"
else
    echo "✗ Database connection failed"
    echo ""
    echo "Please ensure the database and user are created using:"
    echo "sudo -u postgres psql << 'EOF'"
    echo "CREATE DATABASE $DB_NAME;"
    echo "CREATE USER $DB_USER WITH PASSWORD '$DB_PASSWORD';"
    echo "GRANT ALL PRIVILEGES ON DATABASE $DB_NAME TO $DB_USER;"
    echo "\c $DB_NAME"
    echo "GRANT ALL ON SCHEMA public TO $DB_USER;"
    echo "ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO $DB_USER;"
    echo "ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO $DB_USER;"
    echo "EOF"
    exit 1
fi

# Create tables if they don't exist
echo "Creating tables..."
psql -h $DB_HOST -U $DB_USER -p $DB_PORT -d $DB_NAME << 'EOF'
CREATE TABLE IF NOT EXISTS patients (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    contact VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_patients_name ON patients(name);

SELECT 'Tables ready' as status;
EOF

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
