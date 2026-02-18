#!/bin/bash

# Load .env file
if [ -f .env ]; then
    set -a
    source .env
    set +a
else
    echo "Error: .env file not found"
    exit 1
fi

echo "Testing PostgreSQL Connection..."
echo "Host: $DB_HOST"
echo "Port: $DB_PORT"
echo "User: $DB_USER"
echo "Database: $DB_NAME"
echo ""

# Test connection using PGPASSWORD
export PGPASSWORD=$DB_PASSWORD

# Test with psql in non-interactive mode
timeout 5 psql -h $DB_HOST -U $DB_USER -p $DB_PORT -d postgres -t -c "SELECT 'Connection Successful!' as status;" 2>&1

if [ $? -eq 0 ]; then
    echo "✓ PostgreSQL connection test PASSED"
else
    echo "✗ PostgreSQL connection test FAILED"
    exit 1
fi
