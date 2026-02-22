-- 1. Insert Doctors (Assuming 'name' is unique, or just using DO NOTHING for primary key safety)
INSERT INTO doctors (name, specialization, is_available) VALUES
                                                             ('Dr. Alice Smith', 'Cardiology', true),
                                                             ('Dr. Bob Wilson', 'General Medicine', true)
    ON CONFLICT DO NOTHING;

-- 2. Insert Patients (Conflict check on phone_number which has a UNIQUE constraint)
INSERT INTO patients (full_name, phone_number, gender, medical_history_blob) VALUES
                                                                                 ('John Doe', '9876543210', 'Male', 'Hypertension, Allergic to Penicillin'),
                                                                                 ('Jane Miller', '9123456789', 'Female', 'No significant history')
    ON CONFLICT (phone_number) DO NOTHING;

-- 3. Insert Medicines (Conflict check on medicine name)
INSERT INTO medicines (name, stock_count, min_threshold, expiry_date) VALUES
                                                                          ('Paracetamol 500mg', 100, 20, '2027-12-31'),
                                                                          ('Amoxicillin', 5, 10, '2026-06-30'),
                                                                          ('Ibuprofen', 50, 10, '2024-01-01')
    ON CONFLICT DO NOTHING;

-- 4. Create an Appointment for testing (ID 1)
-- We use a check to ensure we don't duplicate the same appointment slot
INSERT INTO appointments (appointment_id, patient_id, doctor_id, appointment_time, status) VALUES
    (1, 1, 1, '2026-03-01 10:00:00', 'BOOKED')
    ON CONFLICT (appointment_id) DO NOTHING;

-- 5. Create a PENDING Prescription (ID 1)
INSERT INTO prescriptions (prescription_id, appointment_id, diagnosis_notes, status, created_at) VALUES
    (1, 1, 'Common Cold and Fever', 'PENDING', CURRENT_TIMESTAMP)
    ON CONFLICT (prescription_id) DO NOTHING;

-- 6. Insert Prescription Items
INSERT INTO prescription_items (prescription_id, medicine_id, quantity) VALUES
                                                                            (1, 1, 2),
                                                                            (1, 2, 10)
    ON CONFLICT DO NOTHING;
INSERT INTO users (username, password, role) VALUES
                                                 ('admin_rec', '$2a$10$8.UnVuG9HHgffUDAlk8qnOn52G3Wy7Mv9mIDpXU6sO8q9r5N66vS.', 'RECEPTIONIST'),
                                                 ('doc_alice', '$2a$10$8.UnVuG9HHgffUDAlk8qnOn52G3Wy7Mv9mIDpXU6sO8q9r5N66vS.', 'DOCTOR'),
                                                 ('pharm_bob', '$2a$10$8.UnVuG9HHgffUDAlk8qnOn52G3Wy7Mv9mIDpXU6sO8q9r5N66vS.', 'PHARMACIST')
    ON CONFLICT DO NOTHING;