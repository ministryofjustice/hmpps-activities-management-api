insert into appointment_series (appointment_series_id, appointment_type, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, dps_location_id, start_date, start_time, end_time, created_time, created_by) values
    (1, 'INDIVIDUAL', 'OTH', 'AC1', 1, 1, false, null, now()::date, '09:00', '10:30', now()::timestamp, 'OTHER.USER');

insert into appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, custom_name, internal_location_id, in_cell, dps_location_id, start_date, start_time, end_time, created_time, created_by) values
    (1, 1, 1, 'MDI', 'AC1', 'Appointment description', 1, false, null, now()::date + 1, '09:00', '09:15', now()::timestamp, 'TEST.USER'),
    (2, 1, 1, 'MDI', 'AC1', 'Appointment description', null, true, null, now()::date + 1, '09:00', '09:15', now()::timestamp, 'TEST.USER'),
    (3, 1, 1, 'MDI', 'AC1', 'Appointment description', 2, false, '99999999-9999-9999-9999-999999999999', now()::date + 1, '09:00', '09:15', now()::timestamp, 'TEST.USER'),
    (4, 1, 1, 'MDI', 'AC1', 'Appointment description', 2, false, null, now()::date + 1, '09:00', '09:15', now()::timestamp, 'TEST.USER'),
    (5, 1, 1, 'MDI', 'AC1', 'Appointment description', 3, false, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', now()::date + 1, '09:00', '09:15', now()::timestamp, 'TEST.USER'),
    (6, 1, 1, 'MDI', 'AC1', 'Appointment description', 4, false, null, now()::date + 1, '09:00', '09:15', now()::timestamp, 'TEST.USER'),
    (7, 1, 1, 'MDI', 'AC1', 'Appointment description', 5, false, null, now()::date + 1, '09:00', '09:15', now()::timestamp, 'TEST.USER'),
    (8, 1, 1, 'MDI', 'AC1', 'Appointment description', 6, false, null, now()::date + 1, '09:00', '09:15', now()::timestamp, 'TEST.USER');
