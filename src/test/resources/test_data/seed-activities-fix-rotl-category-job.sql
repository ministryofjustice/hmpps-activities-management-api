-- Adds the SAA_ROTL category (which will be added permanently via a future migration) plus two outside work
-- activities that are in an incorrect state and need fixing by the activities-fix-rotl-category job:
--   activity 401 - outside_work is true but the category is still SAA_OTHER
--   activity 402 - outside_work is true, the category is already SAA_ROTL, but on_wing is incorrectly true
insert into activity_category(activity_category_id, code, name, description)
values (10, 'SAA_ROTL', 'Outside activity', 'Temporary absence or ROTL for outside work');

insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, on_wing, off_wing, outside_work, piece_work, pay_per_session, summary, description, start_date, end_date, risk_level, created_time, created_by, paid)
values (401, 'MDI', 9, 1, true, false, false, false, true, true, 'H', 'Outside work - wrong category', 'Outside work - wrong category', '2022-10-10', null, 'high', '2022-9-21 00:00:00', 'SEED USER', false);

insert into activity_schedule(activity_schedule_id, activity_id, description, capacity, start_date)
values (401, 401, 'Outside work - wrong category AM', 10, '2022-10-10');

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag, tuesday_flag, wednesday_flag, thursday_flag, friday_flag, time_slot)
values (401, 401, '09:00:00', '11:00:00', true, true, true, true, true, 'AM');

insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, on_wing, off_wing, outside_work, piece_work, pay_per_session, summary, description, start_date, end_date, risk_level, created_time, created_by, paid)
values (402, 'MDI', 10, 1, true, false, true, false, true, true, 'H', 'Outside work - wrong location flags', 'Outside work - wrong location flags', '2022-10-10', null, 'high', '2022-9-21 00:00:00', 'SEED USER', false);

insert into activity_schedule(activity_schedule_id, activity_id, description, capacity, start_date)
values (402, 402, 'Outside work - wrong location flags AM', 10, '2022-10-10');

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag, tuesday_flag, wednesday_flag, thursday_flag, friday_flag, time_slot)
values (402, 402, '09:00:00', '11:00:00', true, true, true, true, true, 'AM');
