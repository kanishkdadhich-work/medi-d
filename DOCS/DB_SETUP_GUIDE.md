# PostgreSQL Setup Instructions for Medi-D

## Prerequisites

- PostgreSQL installed and running on your system
- psql CLI tool available
- Java 21+ and Maven installed

## Quick Setup

### 1. Verify PostgreSQL is Running

```bash
# On Linux/Mac
sudo service postgresql status

# On Windows (if installed as service)
# Check Services app or use: pg_isready
```

### 2. Configure Environment Variables

The project uses a `.env` file for database configuration. An example file is provided:

```bash
# Copy the example to create your actual .env file
cp .env.example .env

# Edit .env with your PostgreSQL credentials (if different)
# Default values point to localhost with postgres/postgres
```

### 3. Run Database Setup Script

```bash
# Make the script executable (Linux/Mac)
chmod +x setup-db.sh

# Run the setup script
./setup-db.sh
```

### 4. Alternative Manual Setup

If the script doesn't work, you can set up the database manually:

```bash
# Connect to PostgreSQL as postgres user
psql -U postgres

# Create database
CREATE DATABASE medid_db;

# Connect to the new database
\c medid_db

# Create table
CREATE TABLE patients (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    contact VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

# Create index
CREATE INDEX idx_patients_name ON patients(name);

# Exit
\q
```

## Running the Application

### 1. Build the Project

```bash
mvn clean install
```

### 2. Run the Application

```bash
# The dev profile will be automatically activated
mvn spring-boot:run
```

Or run directly from your IDE (IntelliJ IDEA will detect the application and provide a Run button).

## Testing the Endpoints

### Health Check

```bash
curl http://localhost:8080/api/health
# Response: Medi-D System is up and running!
```

### Get Patient (Initially no data exists)

```bash
curl http://localhost:8080/api/patients/1
# Response: {"id":1,"name":"Patient not found"}
```

### Insert Sample Data

```bash
# Connect to PostgreSQL
psql -U postgres -d medid_db

# Insert sample patient
INSERT INTO patients (name, contact) VALUES ('John Doe', '+1-555-0100');
INSERT INTO patients (name, contact) VALUES ('Jane Smith', '+1-555-0101');

# Get all patients
SELECT * FROM patients;

# Exit
\q
```

### Get Patient with Data

```bash
curl http://localhost:8080/api/patients/1
# Response: {"id":1,"name":"John Doe"}
```

## Environment Variables Reference

Configure these in your `.env` file:

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_HOST` | localhost | PostgreSQL server hostname |
| `DB_PORT` | 5432 | PostgreSQL server port |
| `DB_NAME` | medid_db | Database name to create/use |
| `DB_USER` | postgres | PostgreSQL username |
| `DB_PASSWORD` | postgres | PostgreSQL password |

## Application Profiles

The application uses Spring profiles for environment-specific configuration:

- **dev** profile: Configured with environment variables
  - File: `application-dev.properties`
  - Hibernate DDL: `update` (auto-updates schema)
  - SQL logging: Enabled for debugging

To use a different profile:

```bash
# In application.properties, change:
spring.profiles.active=dev
# to:
spring.profiles.active=prod
```

## Troubleshooting

### Connection Error: "Connection refused"

- PostgreSQL is not running
- Check hostname and port in `.env`
- Verify PostgreSQL is listening on the correct port

### Authentication Failed: "password authentication failed"

- Check `DB_USER` and `DB_PASSWORD` in `.env`
- Verify PostgreSQL user exists and has correct password

### Database Already Exists

- The setup script will skip database creation if it already exists
- Existing tables will not be re-created

### Port Already in Use (Spring Boot)

Change the port in `application-dev.properties`:

```properties
server.port=8081
```

## Next Steps

1. Implement additional entity models (Pharmacy, OPD, etc.)
2. Add service layer methods for CRUD operations
3. Create update/delete endpoints in controller
4. Add input validation with `@Valid` and DTOs
5. Implement error handling and exception mappers
6. Add Flyway for database migration management
