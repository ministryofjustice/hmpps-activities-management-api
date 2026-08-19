-- Seeds two outside work activities that are in an incorrect state and need fixing by the activities-fix-rotl-category job.
-- Each activity has multiple schedules with populated internal location details and DPS location IDs so the job can
-- prove that all schedule locations are cleared:
--   activity 401 - outside_work is true but the category is still SAA_OTHER
--   activity 402 - outside_work is true, the category is already SAA_ROTL, but on_wing is incorrectly true
--   activity schedule 401 & 402 has internal location information.
--   activity schedule 411 & 412 also has internal location information.

insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, on_wing, off_wing, outside_work, piece_work, pay_per_session, summary, description, start_date, end_date, risk_level, created_time, created_by, paid)
values (401, 'MDI', 9, 1, true, false, false, false, true, true, 'H', 'Outside work - wrong category', 'Outside work - wrong category', '2022-10-10', null, 'high', '2022-9-21 00:00:00', 'SEED USER', false);

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, dps_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (401, 401, 'Outside work - wrong category AM', 101, '11111111-1111-1111-1111-111111111111', 'MDI-INT-1', 'Some internal location 1', 10, '2022-10-10');

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, dps_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (411, 401, 'Outside work - wrong category PM', 102, '11111111-1111-1111-1111-111111111112', 'MDI-INT-2', 'Some internal location 2', 10, '2022-10-10');

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag, tuesday_flag, wednesday_flag, thursday_flag, friday_flag, time_slot)
values (401, 401, '09:00:00', '11:00:00', true, true, true, true, true, 'AM');

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag, tuesday_flag, wednesday_flag, thursday_flag, friday_flag, time_slot)
values (411, 411, '13:00:00', '15:00:00', true, true, true, true, true, 'PM');

insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, on_wing, off_wing, outside_work, piece_work, pay_per_session, summary, description, start_date, end_date, risk_level, created_time, created_by, paid)
values (402, 'MDI', 10, 1, true, false, true, false, true, true, 'H', 'Outside work - wrong location flags', 'Outside work - wrong location flags', '2022-10-10', null, 'high', '2022-9-21 00:00:00', 'SEED USER', false);

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, dps_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (402, 402, 'Outside work - wrong location flags AM', 201, '22222222-2222-2222-2222-222222222221', 'MDI-INT-3', 'Some internal location 3', 10, '2022-10-10');

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, dps_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (412, 402, 'Outside work - wrong location flags PM', 202, '22222222-2222-2222-2222-222222222222', 'MDI-INT-4', 'Some internal location 4', 10, '2022-10-10');

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag, tuesday_flag, wednesday_flag, thursday_flag, friday_flag, time_slot)
values (402, 402, '09:00:00', '11:00:00', true, true, true, true, true, 'AM');

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag, tuesday_flag, wednesday_flag, thursday_flag, friday_flag, time_slot)
values (412, 412, '13:00:00', '15:00:00', true, true, true, true, true, 'PM');
