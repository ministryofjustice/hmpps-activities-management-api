insert into appointment_series (appointment_series_id, appointment_type, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, dps_location_id, start_date, start_time, end_time, created_time, created_by) values
    (1, 'INDIVIDUAL', 'OTH', 'OIC', 1, 1, false, null, now()::date, '09:00', '10:30', now()::timestamp, 'OTHER.USER'),
    (2, 'INDIVIDUAL', 'OTH', 'OIC', 1, null, true, null,now()::date, '09:00', '10:30', now()::timestamp, 'OTHER.USER'),
    (3, 'INDIVIDUAL', 'OTH', 'OIC', 1, 2, true, '99999999-9999-9999-9999-999999999999', now()::date, '09:00', '10:30', now()::timestamp, 'OTHER.USER'),
    (4, 'INDIVIDUAL', 'OTH', 'OIC', 1, 2, false, null, now()::date, '09:00', '10:30', now()::timestamp, 'OTHER.USER'),
    (5, 'INDIVIDUAL', 'OTH', 'OIC', 1, 3, false, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', now()::date, '09:00', '10:30', now()::timestamp, 'OTHER.USER'),
    (6, 'INDIVIDUAL', 'OTH', 'OIC', 1, 4, false, null, now()::date, '09:00', '10:30', now()::timestamp, 'OTHER.USER'),
    (7, 'INDIVIDUAL', 'OTH', 'OIC', 1, 5, false, null, now()::date, '09:00', '10:30', now()::timestamp, 'OTHER.USER'),
    (8, 'INDIVIDUAL', 'OTH', 'OIC', 1, 6, false, null, now()::date, '09:00', '10:30', now()::timestamp, 'OTHER.USER');
