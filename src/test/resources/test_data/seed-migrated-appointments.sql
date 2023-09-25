INSERT INTO appointment_series (appointment_series_id, appointment_type, prison_code, category_code, appointment_tier_id, internal_location_id, start_date, start_time, end_time, created_time, created_by, is_migrated)
VALUES
    (1, 'INDIVIDUAL', 'RSI', 'CHAP', 4, 123, now()::date, '09:00', '10:30', now()::timestamp, 'DPSUSER', false),
    (2, 'INDIVIDUAL', 'RSI', 'CHAP', 4, 123, now()::date, '09:00', '10:30', now()::timestamp, 'DPSUSER', true),
    (3, 'INDIVIDUAL', 'MDI', 'CHAP', 4, 456, now()::date, '09:00', '10:30', now()::timestamp, 'DPSUSER', true),
    (4, 'INDIVIDUAL', 'RSI', 'EDUC', 4, 123, now()::date, '09:00', '10:30', now()::timestamp, 'DPSUSER', true),
    (5, 'INDIVIDUAL', 'RSI', 'CHAP', 4, 123, now()::date + 1, '09:00', '10:30', now()::timestamp, 'DPSUSER', false),
    (6, 'INDIVIDUAL', 'RSI', 'CHAP', 4, 123, now()::date + 1, '09:00', '10:30', now()::timestamp, 'DPSUSER', true),
    (7, 'INDIVIDUAL', 'MDI', 'CHAP', 4, 456, now()::date + 1, '09:00', '10:30', now()::timestamp, 'DPSUSER', true),
    (8, 'INDIVIDUAL', 'RSI', 'EDUC', 4, 123, now()::date + 1, '09:00', '10:30', now()::timestamp, 'DPSUSER', true);

INSERT INTO appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, appointment_tier_id, internal_location_id, start_date, start_time, end_time, created_time, created_by)
VALUES
    (10, 1, 1, 'RSI', 'CHAP', 4, 123, now()::date, '09:00', '10:30', now()::timestamp, 'DPSUSER'),
    (11, 2, 1, 'RSI', 'CHAP', 4, 123, now()::date, '09:00', '10:30', now()::timestamp, 'DPSUSER'),
    (12, 3, 1, 'MDI', 'CHAP', 4, 456, now()::date, '09:00', '10:30', now()::timestamp, 'DPSUSER'),
    (13, 4, 1, 'RSI', 'EDUC', 4, 123, now()::date, '09:00', '10:30', now()::timestamp, 'DPSUSER'),
    (14, 5, 1, 'RSI', 'CHAP', 4, 123, now()::date + 1, '09:00', '10:30', now()::timestamp, 'DPSUSER'),
    (15, 6, 1, 'RSI', 'CHAP', 4, 123, now()::date + 1, '09:00', '10:30', now()::timestamp, 'DPSUSER'),
    (16, 7, 1, 'MDI', 'CHAP', 4, 456, now()::date + 1, '09:00', '10:30', now()::timestamp, 'DPSUSER'),
    (17, 8, 1, 'RSI', 'EDUC', 4, 123, now()::date + 1, '09:00', '10:30', now()::timestamp, 'DPSUSER');

INSERT INTO appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id)
VALUES
    (20, 10, 'A1234BC', 123),
    (21, 11, 'A1234BC', 123),
    (22, 12, 'B2345CD', 456),
    (23, 13, 'A1234BC', 123),
    (24, 14, 'A1234BC', 123),
    (25, 15, 'A1234BC', 123),
    (26, 16, 'B2345CD', 456),
    (27, 17, 'A1234BC', 123);
