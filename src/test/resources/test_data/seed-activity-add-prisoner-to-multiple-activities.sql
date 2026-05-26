insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, created_time, created_by, paid)
values (1, 'MDI', 1, 1, true, false, false, false, 'H', 'Maths', 'Maths Level 1', '2022-10-10', null, 'high', '2022-9-21 00:00:00', 'SEED USER', false);

insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, created_time, created_by, paid)
values (2, 'MDI', 1, 1, true, false, false, false, 'H', 'English', 'English Level 1', '2022-10-10', null, 'high', '2022-9-21 00:00:00', 'SEED USER', false);

insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, created_time, created_by, paid)
values (3, 'MDI', 1, 1, true, false, false, false, 'H', 'Science', 'Science Level 1', '2022-10-10', null, 'high', '2022-9-21 00:00:00', 'SEED USER', false);

insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, created_time, created_by, paid)
values (4, 'MDI', 1, 1, true, false, false, false, 'H', 'French', 'French Level 1', '2022-10-10', null, 'high', '2022-9-21 00:00:00', 'SEED USER', false);

insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, created_time, created_by, paid)
values (5, 'MDI', 1, 1, true, false, false, false, 'H', 'Spanish', 'Spanish Level 1', '2022-10-10', null, 'high', '2022-9-21 00:00:00', 'SEED USER', false);

insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, created_time, created_by, paid)
values (6, 'MDI', 1, 1, true, false, false, true, 'H', 'Woodwork', 'Woodwork Level 1', '2022-10-10', null, 'high', '2022-9-21 00:00:00', 'SEED USER', false);

insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, created_time, created_by, paid)
values (7, 'PVI', 1, 1, true, false, false, true, 'H', 'Metalwork', 'Metalwork Level 1', '2022-10-10', null, 'high', '2022-9-21 00:00:00', 'SEED USER', false);

insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, created_time, created_by, paid)
values (8, 'PVI', 1, 1, true, false, false, false, 'H', 'Cleaning', 'Cleaning', '2022-10-10', null, 'high', '2022-9-21 00:00:00', 'SEED USER', false);

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (1, 1, 'Maths AM', 1, 'L1', 'Location 1', 10, '2022-10-10');

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag, time_slot)
values (1, 1, '09:00:00', '12:00:00', true, 'AM');

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (2, 2, 'English PM', 1, 'L1', 'Location 1', 10, '2022-10-10');

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag, time_slot)
values (2, 2, '13:00:00', '14:00:00', true, 'AM');

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (3, 3, 'Science PM', 1, 'L1', 'Location 1', 10, '2022-10-10');

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag, time_slot)
values (3, 3, '14:00:00', '15:00:00', true, 'AM');

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (4, 4, 'French PM', 1, 'L1', 'Location 1', 10, '2022-10-10');

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag, time_slot)
values (4, 4, '15:00:00', '16:00:00', true, 'AM');

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (5, 5, 'Spanish PM', 1, 'L1', 'Location 1', 10, '2022-10-10');

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag, time_slot)
values (5, 5, '16:00:00', '17:00:00', true, 'AM');

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (6, 6, 'Woodwork', 1, 'L1', 'Location 1', 10, '2022-10-10');

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag, time_slot)
values (6, 5, '16:00:00', '17:00:00', true, 'AM');

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (7, 7, 'Metalwork', 1, 'L1', 'Metalwork 1', 10, '2022-10-10');

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag, time_slot)
values (7, 7, '16:00:00', '17:00:00', true, 'AM');

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (8, 8, 'Cleaning', 1, 'L1', 'Cleaning', 10, '2022-10-10');

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag, time_slot)
values (8, 8, '16:00:00', '17:00:00', true, 'AM');
