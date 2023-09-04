INSERT INTO appointment (appointment_id, appointment_type, category_code, appointment_description, prison_code, internal_location_id, in_cell, start_date, start_time, end_time, comment, created, created_by)
VALUES (3, 'INDIVIDUAL', 'AC1', 'Appointment description', 'MDI', 123, false, '2022-10-01', '09:00', '10:30', 'Appointment level comment', now()::timestamp, 'TEST.USER');

INSERT INTO appointment_occurrence (appointment_occurrence_id, appointment_id, sequence_number, category_code, appointment_description, internal_location_id, in_cell, start_date, start_time, end_time, comment)
VALUES (3, 3, 1, 'AC1', 'Appointment description', 123, false, '2022-10-01', '09:00', '10:30', 'Appointment occurrence level comment');

INSERT INTO appointment_occurrence_allocation (appointment_occurrence_allocation_id, appointment_occurrence_id, prisoner_number, booking_id)
VALUES (4, 3, 'A11111A', 1200993);
