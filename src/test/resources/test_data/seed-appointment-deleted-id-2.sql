INSERT INTO appointment (appointment_id, category_code, prison_code, internal_location_id, in_cell, start_date, start_time, end_time, comment, created, created_by, deleted)
VALUES (1, 'AC1', 'TPR', 123, false, now()::date, '09:00', '10:30', 'Appointment level comment', now()::timestamp, 'TEST.USER', true);

INSERT INTO appointment_occurrence (appointment_occurrence_id, appointment_id, sequence_number, internal_location_id, in_cell, start_date, start_time, end_time, comment)
VALUES (2, 1, 1, 123, false, now()::date, '09:00', '10:30', 'Appointment occurrence level comment');

INSERT INTO appointment_occurrence_allocation (appointment_occurrence_allocation_id, appointment_occurrence_id, prisoner_number, booking_id)
VALUES (3, 2, 'A1234BC', 456);
