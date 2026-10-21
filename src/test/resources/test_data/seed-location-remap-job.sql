INSERT INTO activity (activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, on_wing, off_wing, outside_work, piece_work, pay_per_session, summary, description, start_date, end_date, risk_level, created_time, created_by, paid)
VALUES
    (401, 'TPR', 9, 1, true, false, false, false, false, false, 'H', 'Laundry', 'Laundry', current_date - 30, null, 'low', now(), 'TEST.USER', false),
    (402, 'TPR', 9, 1, true, false, false, false, false, false, 'H', 'Ended', 'Ended', current_date - 30, current_date - 1, 'low', now(), 'TEST.USER', false),
    (403, 'TPR', 9, 1, true, false, false, false, false, false, 'H', 'Planned', 'Planned', current_date + 30, null, 'low', now(), 'TEST.USER', false);

INSERT INTO activity_schedule (activity_schedule_id, activity_id, description, internal_location_id, dps_location_id, internal_location_code, internal_location_description, capacity, start_date)
VALUES
    (401, 401, 'Laundry', 123, '44444444-1111-2222-3333-444444444444', 'PARENT', 'Laundry', 10, current_date - 30),
    (402, 402, 'Ended', 123, '44444444-1111-2222-3333-444444444444', 'PARENT', 'Laundry', 10, current_date - 30),
    (403, 403, 'Planned', 123, '44444444-1111-2222-3333-444444444444', 'PARENT', 'Laundry', 10, current_date + 30),
    (404, 401, 'Override', 321, '66666666-6666-6666-6666-666666666666', 'OVERRIDE', 'Other room', 10, current_date - 30);

UPDATE appointment_series SET start_date = current_date - 100 WHERE appointment_series_id = 5;
UPDATE appointment SET start_date = current_date - 100 WHERE appointment_id = 10;
UPDATE appointment SET cancelled_time = now(), cancelled_by = 'TEST.USER' WHERE appointment_id = 12;
UPDATE appointment SET dps_location_id = '66666666-6666-6666-6666-666666666666', internal_location_id = 321 WHERE appointment_id = 13;
UPDATE appointment_set SET dps_location_id = '44444444-1111-2222-3333-444444444444' WHERE appointment_set_id = 6;
UPDATE appointment_series SET dps_location_id = '44444444-1111-2222-3333-444444444444' WHERE appointment_series_id IN (6, 7, 8);
UPDATE appointment SET dps_location_id = '44444444-1111-2222-3333-444444444444' WHERE appointment_id IN (6, 7, 8);
UPDATE appointment SET start_date = current_date WHERE appointment_id = 7;
UPDATE appointment SET is_deleted = true WHERE appointment_id = 8;