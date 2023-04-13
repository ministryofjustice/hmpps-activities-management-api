INSERT INTO appointment_schedule (appointment_schedule_id, repeat_period, repeat_count)
VALUES (1, 'WEEKLY', 4);

INSERT INTO appointment (appointment_id, appointment_type, category_code, prison_code, internal_location_id, in_cell, start_date, start_time, end_time, appointment_schedule_id, comment, created, created_by)
VALUES (5, 'GROUP', 'AC1', 'TPR', 123, false, now()::date - 3, '09:00', '10:30', 1, 'Appointment level comment', now()::timestamp, 'TEST.USER');

INSERT INTO appointment_occurrence (appointment_occurrence_id, appointment_id, sequence_number, internal_location_id, in_cell, start_date, start_time, end_time, comment)
VALUES (10, 5, 1, 123, false, now()::date - 3, '09:00', '10:30', 'Appointment occurrence level comment');
INSERT INTO appointment_occurrence (appointment_occurrence_id, appointment_id, sequence_number, internal_location_id, in_cell, start_date, start_time, end_time, comment)
VALUES (11, 5, 2, 123, false, now()::date + 4, '09:00', '10:30', 'Appointment occurrence level comment');
INSERT INTO appointment_occurrence (appointment_occurrence_id, appointment_id, sequence_number, internal_location_id, in_cell, start_date, start_time, end_time, comment)
VALUES (12, 5, 3, 123, false, now()::date + 11, '09:00', '10:30', 'Appointment occurrence level comment');
INSERT INTO appointment_occurrence (appointment_occurrence_id, appointment_id, sequence_number, internal_location_id, in_cell, start_date, start_time, end_time, comment)
VALUES (13, 5, 4, 123, false, now()::date + 18, '09:00', '10:30', 'Appointment occurrence level comment');

INSERT INTO appointment_occurrence_allocation (appointment_occurrence_allocation_id, appointment_occurrence_id, prisoner_number, booking_id)
VALUES (20, 10, 'A1234BC', 456);
INSERT INTO appointment_occurrence_allocation (appointment_occurrence_allocation_id, appointment_occurrence_id, prisoner_number, booking_id)
VALUES (21, 10, 'B2345CD', 457);
INSERT INTO appointment_occurrence_allocation (appointment_occurrence_allocation_id, appointment_occurrence_id, prisoner_number, booking_id)
VALUES (22, 11, 'A1234BC', 456);
INSERT INTO appointment_occurrence_allocation (appointment_occurrence_allocation_id, appointment_occurrence_id, prisoner_number, booking_id)
VALUES (23, 11, 'B2345CD', 457);
INSERT INTO appointment_occurrence_allocation (appointment_occurrence_allocation_id, appointment_occurrence_id, prisoner_number, booking_id)
VALUES (24, 12, 'A1234BC', 456);
INSERT INTO appointment_occurrence_allocation (appointment_occurrence_allocation_id, appointment_occurrence_id, prisoner_number, booking_id)
VALUES (25, 12, 'B2345CD', 457);
INSERT INTO appointment_occurrence_allocation (appointment_occurrence_allocation_id, appointment_occurrence_id, prisoner_number, booking_id)
VALUES (26, 13, 'A1234BC', 456);
INSERT INTO appointment_occurrence_allocation (appointment_occurrence_allocation_id, appointment_occurrence_id, prisoner_number, booking_id)
VALUES (27, 13, 'B2345CD', 457);

