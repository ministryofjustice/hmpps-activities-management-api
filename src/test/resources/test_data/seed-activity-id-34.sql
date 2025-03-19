--
-- Test data for find activities with invalid locations --

-- Ignore because dps_location_id is valid
insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, created_time, created_by, paid)
values (1, 'RSI', 1, 1, true, false, false, false, 'H', 'Activity 1', 'Activity 1', current_timestamp, null, 'high', '2022-9-21 00:00:00', 'SEED USER', true);

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date, end_date, dps_location_id)
values (1, 1, 'Activity 1', 1, 'L1', 'Location 1', 10, current_timestamp, null, '11111111-1111-1111-1111-111111111111');

-- Ignore because activity has ended
insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, created_time, created_by, paid)
values (2, 'RSI', 1, 1, true, false, false, false, 'H', 'Activity 2', 'Activity 2', current_timestamp - interval '2 day', current_timestamp - interval '1 day', 'high', '2022-9-21 00:00:00', 'SEED USER', true);

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date, end_date, dps_location_id)
values (2, 2, 'Activity 2', 2, 'L2', 'Location 2', 10, current_timestamp, null, '22222222-2222-2222-2222-222222222222');

-- Error because dps_location_id is invalid and activity has not ended
insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, created_time, created_by, paid)
values (3, 'RSI', 1, 1, true, false, false, false, 'H', 'Activity 3', 'Activity 3', current_timestamp - interval '1 day', current_timestamp, 'high', '2022-9-21 00:00:00', 'SEED USER', true);

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date, end_date, dps_location_id)
values (3, 3, 'Activity 3', 2, 'L2', 'Location 2', 10, current_timestamp, null, '22222222-2222-2222-2222-222222222222');

