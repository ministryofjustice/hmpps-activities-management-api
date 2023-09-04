INSERT INTO appointment (appointment_id, appointment_type, category_code, prison_code, internal_location_id, in_cell, start_date, start_time, end_time, comment, created, created_by)
VALUES (2, 'INDIVIDUAL', 'AC1', 'TPR', 123, false, now()::date + 1, '09:00', '10:30', 'Appointment level comment', now()::timestamp, 'TEST.USER');

INSERT INTO appointment_occurrence (appointment_occurrence_id, appointment_id, sequence_number, category_code, internal_location_id, in_cell, start_date, start_time, end_time, comment, cancelled, cancellation_reason_id, cancelled_by, deleted)
VALUES (3, 2, 1, 'AC1', 123, false, now()::date + 1, '09:00', '10:30', 'Appointment occurrence level comment', now()::timestamp, 1, 'TEST.USER', true);

INSERT INTO appointment_occurrence_allocation (appointment_occurrence_allocation_id, appointment_occurrence_id, prisoner_number, booking_id)
VALUES (4, 3, 'A1234BC', 456);
