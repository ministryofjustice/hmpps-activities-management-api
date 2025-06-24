insert into appointment_set (appointment_set_id, prison_code, category_code, custom_name, appointment_tier_id, appointment_organiser_id, internal_location_id, in_cell, dps_location_id, start_date, created_time, created_by) values
    (1, 'MDI', 'AC1', 'Appointment description', 2, 1, 1, false, null, now()::date + 1, now()::timestamp, 'TEST.USER'),
    (2, 'MDI', 'AC1', 'Appointment description', 2, 1, null, true, null, now()::date + 1, now()::timestamp, 'TEST.USER'),
    (3, 'MDI', 'AC1', 'Appointment description', 2, 1, 2, false, '99999999-9999-9999-9999-999999999999', now()::date + 1, now()::timestamp, 'TEST.USER'),
    (4, 'MDI', 'AC1', 'Appointment description', 2, 1, 2, false, null, now()::date + 1, now()::timestamp, 'TEST.USER'),
    (5, 'MDI', 'AC1', 'Appointment description', 2, 1, 3, false, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', now()::date + 1, now()::timestamp, 'TEST.USER'),
    (6, 'MDI', 'AC1', 'Appointment description', 2, 1, 4, false, null, now()::date + 1, now()::timestamp, 'TEST.USER'),
    (7, 'MDI', 'AC1', 'Appointment description', 2, 1, 5, false, null, now()::date + 1, now()::timestamp, 'TEST.USER'),
    (8, 'MDI', 'AC1', 'Appointment description', 2, 1, 6, false, null, now()::date + 1, now()::timestamp, 'TEST.USER');
