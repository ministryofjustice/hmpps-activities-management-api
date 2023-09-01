INSERT INTO appointment_schedule (appointment_schedule_id, repeat_period, repeat_count)
VALUES (2, 'WEEKLY', 4);

INSERT INTO appointment (appointment_id, appointment_type, category_code, prison_code, internal_location_id, in_cell, start_date, start_time, end_time, appointment_schedule_id, comment, created, created_by)
VALUES (7, 'GROUP', 'AC1', 'TPR', 123, false, now()::date + 1, '09:00', '10:30', 2, 'Appointment level comment', now()::timestamp, 'TEST.USER');

INSERT INTO appointment_occurrence (appointment_occurrence_id, appointment_id, sequence_number, internal_location_id, in_cell, start_date, start_time, end_time, comment)
VALUES (20, 7, 1, 123, false, now()::date + 1, '09:00', '10:30', 'Appointment occurrence level comment');
INSERT INTO appointment_occurrence (appointment_occurrence_id, appointment_id, sequence_number, internal_location_id, in_cell, start_date, start_time, end_time, comment)
VALUES (21, 7, 2, 123, false, now()::date + 8, '09:00', '10:30', 'Appointment occurrence level comment');
INSERT INTO appointment_occurrence (appointment_occurrence_id, appointment_id, sequence_number, internal_location_id, in_cell, start_date, start_time, end_time, comment)
VALUES (22, 7, 3, 123, false, now()::date + 15, '09:00', '10:30', 'Appointment occurrence level comment');
INSERT INTO appointment_occurrence (appointment_occurrence_id, appointment_id, sequence_number, internal_location_id, in_cell, start_date, start_time, end_time, comment)
VALUES (23, 7, 4, 123, false, now()::date + 22, '09:00', '10:30', 'Appointment occurrence level comment');

INSERT INTO appointment_occurrence_allocation (appointment_occurrence_allocation_id, appointment_occurrence_id, prisoner_number, booking_id)
VALUES (30, 20, 'A1234BC', 456);
INSERT INTO appointment_occurrence_allocation (appointment_occurrence_allocation_id, appointment_occurrence_id, prisoner_number, booking_id)
VALUES (31, 20, 'B2345CD', 457);
INSERT INTO appointment_occurrence_allocation (appointment_occurrence_allocation_id, appointment_occurrence_id, prisoner_number, booking_id)
VALUES (32, 20, 'C3456DE', 458);
INSERT INTO appointment_occurrence_allocation (appointment_occurrence_allocation_id, appointment_occurrence_id, prisoner_number, booking_id)
VALUES (33, 21, 'A1234BC', 456);
INSERT INTO appointment_occurrence_allocation (appointment_occurrence_allocation_id, appointment_occurrence_id, prisoner_number, booking_id)
VALUES (34, 21, 'B2345CD', 457);
INSERT INTO appointment_occurrence_allocation (appointment_occurrence_allocation_id, appointment_occurrence_id, prisoner_number, booking_id)
VALUES (35, 21, 'C3456DE', 458);
INSERT INTO appointment_occurrence_allocation (appointment_occurrence_allocation_id, appointment_occurrence_id, prisoner_number, booking_id)
VALUES (36, 22, 'A1234BC', 456);
INSERT INTO appointment_occurrence_allocation (appointment_occurrence_allocation_id, appointment_occurrence_id, prisoner_number, booking_id)
VALUES (37, 22, 'B2345CD', 457);
INSERT INTO appointment_occurrence_allocation (appointment_occurrence_allocation_id, appointment_occurrence_id, prisoner_number, booking_id)
VALUES (38, 22, 'C3456DE', 458);
INSERT INTO appointment_occurrence_allocation (appointment_occurrence_allocation_id, appointment_occurrence_id, prisoner_number, booking_id)
VALUES (39, 23, 'A1234BC', 456);
INSERT INTO appointment_occurrence_allocation (appointment_occurrence_allocation_id, appointment_occurrence_id, prisoner_number, booking_id)
VALUES (40, 23, 'B2345CD', 457);
INSERT INTO appointment_occurrence_allocation (appointment_occurrence_allocation_id, appointment_occurrence_id, prisoner_number, booking_id)
VALUES (41, 23, 'C3456DE', 458);

