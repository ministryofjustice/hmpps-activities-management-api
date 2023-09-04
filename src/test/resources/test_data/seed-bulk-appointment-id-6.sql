INSERT INTO appointment (appointment_id, appointment_type, category_code, appointment_description, prison_code, internal_location_id, in_cell, start_date, start_time, end_time, comment, created, created_by)
VALUES (6, 'INDIVIDUAL', 'AC1', 'Appointment description', 'TPR', 123, false, now()::date + 1, '09:00', '09:15', 'Medical appointment for A1234BC', now()::timestamp, 'TEST.USER');
INSERT INTO appointment (appointment_id, appointment_type, category_code, appointment_description, prison_code, internal_location_id, in_cell, start_date, start_time, end_time, comment, created, created_by)
VALUES (7, 'INDIVIDUAL', 'AC1', 'Appointment description', 'TPR', 123, false, now()::date + 1, '09:15', '09:30', 'Medical appointment for B2345CD', now()::timestamp, 'TEST.USER');
INSERT INTO appointment (appointment_id, appointment_type, category_code, appointment_description, prison_code, internal_location_id, in_cell, start_date, start_time, end_time, comment, created, created_by)
VALUES (8, 'INDIVIDUAL', 'AC1', 'Appointment description', 'TPR', 123, false, now()::date + 1, '09:30', '09:45', 'Medical appointment for C3456DE', now()::timestamp, 'TEST.USER');

INSERT INTO appointment_occurrence (appointment_occurrence_id, appointment_id, sequence_number, category_code, appointment_description, internal_location_id, in_cell, start_date, start_time, end_time, comment)
VALUES (6, 6, 1, 'AC1', 'Appointment description', 123, false, now()::date + 1, '09:00', '09:15', 'Medical appointment for A1234BC');
INSERT INTO appointment_occurrence (appointment_occurrence_id, appointment_id, sequence_number, category_code, appointment_description, internal_location_id, in_cell, start_date, start_time, end_time, comment)
VALUES (7, 7, 1, 'AC1', 'Appointment description', 123, false, now()::date + 1, '09:15', '09:30', 'Medical appointment for B2345CD');
INSERT INTO appointment_occurrence (appointment_occurrence_id, appointment_id, sequence_number, category_code, appointment_description, internal_location_id, in_cell, start_date, start_time, end_time, comment)
VALUES (8, 8, 1, 'AC1', 'Appointment description', 123, false, now()::date + 1, '09:30', '09:45', 'Medical appointment for C3456DE');

INSERT INTO appointment_occurrence_allocation (appointment_occurrence_allocation_id, appointment_occurrence_id, prisoner_number, booking_id)
VALUES (6, 6, 'A1234BC', 456);
INSERT INTO appointment_occurrence_allocation (appointment_occurrence_allocation_id, appointment_occurrence_id, prisoner_number, booking_id)
VALUES (7, 7, 'B2345CD', 457);
INSERT INTO appointment_occurrence_allocation (appointment_occurrence_allocation_id, appointment_occurrence_id, prisoner_number, booking_id)
VALUES (8, 8, 'C3456DE', 458);

INSERT INTO bulk_appointment (bulk_appointment_id, prison_code, category_code, appointment_description, internal_location_id, in_cell, start_date, created, created_by)
VALUES (6, 'TPR', 'AC1', 'Appointment description', 123, false, now()::date + 1, now()::timestamp, 'TEST.USER');

INSERT INTO bulk_appointment_appointment (bulk_appointment_appointment_id, bulk_appointment_id, appointment_id)
VALUES (6, 6, 6);
INSERT INTO bulk_appointment_appointment (bulk_appointment_appointment_id, bulk_appointment_id, appointment_id)
VALUES (7, 6, 7);
INSERT INTO bulk_appointment_appointment (bulk_appointment_appointment_id, bulk_appointment_id, appointment_id)
VALUES (8, 6, 8);
