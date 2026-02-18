-- V2__Create_clinic_management_tables.sql
-- Database migration for clinic management features

-- Create medicines table
CREATE TABLE IF NOT EXISTS medicines (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    stock INT NOT NULL,
    expiry_date DATE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes on medicines table
CREATE INDEX IF NOT EXISTS idx_medicine_name ON medicines(name);
CREATE INDEX IF NOT EXISTS idx_medicine_expiry ON medicines(expiry_date);

-- Create appointments table
CREATE TABLE IF NOT EXISTS appointments (
    id BIGSERIAL PRIMARY KEY,
    doctor_id BIGINT NOT NULL,
    patient_id BIGINT NOT NULL REFERENCES patients(id),
    slot_timestamp TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes on appointments table
CREATE INDEX IF NOT EXISTS idx_appointment_doctor ON appointments(doctor_id);
CREATE INDEX IF NOT EXISTS idx_appointment_patient ON appointments(patient_id);
CREATE INDEX IF NOT EXISTS idx_appointment_slot ON appointments(slot_timestamp);
CREATE INDEX IF NOT EXISTS idx_appointment_status ON appointments(status);

-- Create prescriptions table
CREATE TABLE IF NOT EXISTS prescriptions (
    id BIGSERIAL PRIMARY KEY,
    appointment_id BIGINT NOT NULL UNIQUE REFERENCES appointments(id),
    diagnosis TEXT,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes on prescriptions table
CREATE INDEX IF NOT EXISTS idx_prescription_appointment ON prescriptions(appointment_id);
CREATE INDEX IF NOT EXISTS idx_prescription_status ON prescriptions(status);

-- Create prescription_items table (join table with additional data)
CREATE TABLE IF NOT EXISTS prescription_items (
    id BIGSERIAL PRIMARY KEY,
    prescription_id BIGINT NOT NULL REFERENCES prescriptions(id),
    medicine_id BIGINT NOT NULL REFERENCES medicines(id),
    quantity_required INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes on prescription_items table
CREATE INDEX IF NOT EXISTS idx_prescriptionitem_prescription ON prescription_items(prescription_id);
CREATE INDEX IF NOT EXISTS idx_prescriptionitem_medicine ON prescription_items(medicine_id);

-- Add foreign key constraints
ALTER TABLE appointments
ADD CONSTRAINT fk_appointment_patient
FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE;

ALTER TABLE prescriptions
ADD CONSTRAINT fk_prescription_appointment
FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE CASCADE;

ALTER TABLE prescription_items
ADD CONSTRAINT fk_prescriptionitem_prescription
FOREIGN KEY (prescription_id) REFERENCES prescriptions(id) ON DELETE CASCADE;

ALTER TABLE prescription_items
ADD CONSTRAINT fk_prescriptionitem_medicine
FOREIGN KEY (medicine_id) REFERENCES medicines(id) ON DELETE RESTRICT;

-- Add comment to medications for documentation
COMMENT ON COLUMN medicines.version IS 'Optimistic locking version - DO NOT modify manually. Prevents concurrent stock modification issues.';

COMMENT ON TABLE medicines IS 'Stores medicine information with stock tracking and expiry date management.';
COMMENT ON TABLE appointments IS 'Stores appointment information linking doctors and patients.';
COMMENT ON TABLE prescriptions IS 'Stores prescription information for each appointment.';
COMMENT ON TABLE prescription_items IS 'Join table linking prescriptions to medicines with required quantities.';
