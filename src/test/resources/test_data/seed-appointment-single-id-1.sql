INSERT INTO appointment (appointment_id, appointment_category_id, prison_code, internal_location_id, in_cell, start_date, start_time, end_time, comment, created, created_by)
VALUES (1, 3, 'TPR', 123, false, now()::date, '09:00', '10:30', 'Appointment level comment', now()::timestamp, 'TEST.USER');

INSERT INTO appointment_occurrence (appointment_occurrence_id, appointment_id, internal_location_id, in_cell, start_date, start_time, end_time, comment)
VALUES (1, 1, 123, false, now()::date, '09:00', '10:30', 'Appointment occurrence level comment');

INSERT INTO appointment_occurrence_allocation (appointment_occurrence_allocation_id, appointment_occurrence_id, prisoner_number, booking_id)
VALUES (1, 1, 'A1234BC', 456);

INSERT INTO appointment_occurrence_allocation (appointment_occurrence_allocation_id, appointment_occurrence_id, prisoner_number, booking_id)
VALUES (1, 1, 'A1234BC', 456);

INSERT INTO appointment_instance (appointment_instance_id, appointment_occurrence_id, appointment_category_id, prison_code, internal_location_id,
                                  in_cell, prisoner_number, booking_id, appointment_date, start_time, end_time, "comment", attended, cancelled)
VALUES(1, 1, 3, 'TPR', 123, false, 'A1234BC', 456, now()::date, '09:00', '10:30', null, null, false);

