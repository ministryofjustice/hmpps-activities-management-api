INSERT INTO prison_regime (prison_regime_id, prison_code, am_start, am_finish, pm_start, pm_finish, ed_start, ed_finish)
VALUES (3, 'PVI', '10:00:00', '11:00:00', '13:00:00', '16:30:00', '18:00:00', '20:00:00');

-- Activity with end_date yesterday → ARCHIVED
insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, created_time, created_by, paid)
values (1, 'PVI', 1, 1, true, false, false, false, 'H', 'Maths', 'Maths Level 1', current_date - interval '10 days', current_date - interval '1 day', 'high', current_date - interval '10 days', 'SEED USER', true);

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (1, 1, 'Maths AM', 1, 'L1', 'Location 1', 10, current_date - interval '10 days');

-- Activity with end_date today → LIVE
insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, created_time, created_by, paid)
values (2, 'PVI', 1, 1, true, false, false, false, 'H', 'English', 'English Level 1', current_date - interval '10 days', current_date, 'high', current_date - interval '10 days', 'SEED USER', true);

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (2, 2, 'English AM', 1, 'L1', 'Location 1', 10, current_date - interval '10 days');

-- Activity with no end_date → LIVE
insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, created_time, created_by, paid)
values (3, 'PVI', 1, 1, true, false, false, false, 'H', 'Science', 'Science Level 1', current_date - interval '10 days', null, 'high', current_date - interval '10 days', 'SEED USER', true);

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (3, 3, 'Science AM', 1, 'L1', 'Location 1', 10, current_date - interval '10 days');

-- Activity with end_date tomorrow → LIVE
insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, created_time, created_by, paid)
values (4, 'PVI', 1, 1, true, false, false, false, 'H', 'History', 'History Level 1', current_date - interval '10 days', current_date + interval '1 day', 'high', current_date - interval '10 days', 'SEED USER', true);

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (4, 4, 'History AM', 1, 'L1', 'Location 1', 10, current_date - interval '10 days');
