INSERT INTO appointment (appointment_id, appointment_type, category_code, prison_code, internal_location_id, in_cell, start_date, start_time, end_time, comment, created, created_by)
VALUES (3, 'INDIVIDUAL', 'AC1', 'MDI', 123, false, '2022-10-01', '09:00', '10:30', 'Appointment level comment', now()::timestamp, 'TEST.USER');

INSERT INTO appointment_occurrence (appointment_occurrence_id, appointment_id, sequence_number, internal_location_id, in_cell, start_date, start_time, end_time, comment)
VALUES (3, 3, 1, 123, false, '2022-10-01', '09:00', '10:30', 'Appointment occurrence level comment');

INSERT INTO appointment_occurrence_allocation (appointment_occurrence_allocation_id, appointment_occurrence_id, prisoner_number, booking_id)
VALUES (4, 3, 'A5193DY', 1200993);

INSERT INTO appointment_occurrence_allocation (appointment_occurrence_allocation_id, appointment_occurrence_id, prisoner_number, booking_id)
VALUES (5, 3, 'G4793VF', 1200994);
