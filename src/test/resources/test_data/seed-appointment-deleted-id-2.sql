INSERT INTO appointment (appointment_id, appointment_category_id, prison_code, internal_location_id, in_cell, start_date, start_time, end_time, comment, created, created_by, deleted)
VALUES (1, 3, 'TPR', 123, false, now()::date, '09:00', '10:30', 'Appointment level comment', now()::timestamp, 'TEST.USER', true);
