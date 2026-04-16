INSERT INTO prison_regime (prison_regime_id, prison_code, am_start, am_finish, pm_start, pm_finish, ed_start, ed_finish)
VALUES(3, 'PVI', '10:00:00', '11:00:00', '13:00:00', '16:30:00', '18:00:00', '20:00:00');

insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, created_time, created_by, paid)
values (1, 'PVI', 1, 1, true, false, false, false, 'H', 'Maths', 'Maths Level 1', '2022-10-10', null, 'high', '2022-9-21 00:00:00', 'SEED USER', false);

insert into activity_minimum_education_level(activity_minimum_education_level_id, activity_id, education_level_code, education_level_description, study_area_code, study_area_description)
values (1, 1, '1', 'Reading Measure 1.0', 'ENGLA', 'English Language');

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (1, 1, 'Maths AM', 1, 'L1', 'Location 1', 10, '2022-10-10');

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag, tuesday_flag, wednesday_flag, thursday_flag, friday_flag, saturday_flag, sunday_flag, time_slot)
values (1, 1, '10:00:00', '11:00:00', true, true, true, true, true, true, true, 'AM');

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag, tuesday_flag, wednesday_flag, thursday_flag, friday_flag, saturday_flag, sunday_flag, time_slot)
values (2, 1, '12:00:00', '13:00:00', true, true, true, true, true, true, true, 'PM');

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag, tuesday_flag, wednesday_flag, thursday_flag, friday_flag, saturday_flag, sunday_flag, time_slot)
values (3, 1, '18:00:00', '19:00:00', true, true, true, true, true, true, true, 'ED');

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (1, 1, 'A1234BC', 10001, null, '2022-10-10', null, '2022-10-10 09:00:00', 'MR BLOGS', null, null, null, null, null, null, 'ACTIVE');

insert into exclusion(allocation_id, time_slot, week_number, start_date, end_date)
values (1, 'AM', 1, current_date, null);

insert into exclusion_days_of_week(exclusion_id, day_of_week) select 1, 'MONDAY' where extract(isodow from current_date) = 1;
insert into exclusion_days_of_week(exclusion_id, day_of_week) select 1, 'TUESDAY' where extract(isodow from current_date) = 2;
insert into exclusion_days_of_week(exclusion_id, day_of_week) select 1, 'WEDNESDAY' where extract(isodow from current_date) = 3;
insert into exclusion_days_of_week(exclusion_id, day_of_week) select 1, 'THURSDAY' where extract(isodow from current_date) = 4;
insert into exclusion_days_of_week(exclusion_id, day_of_week) select 1, 'FRIDAY' where extract(isodow from current_date) = 5;
insert into exclusion_days_of_week(exclusion_id, day_of_week) select 1, 'SATURDAY' where extract(isodow from current_date) = 6;
insert into exclusion_days_of_week(exclusion_id, day_of_week) select 1, 'SUNDAY' where extract(isodow from current_date) = 7;

insert into exclusion(allocation_id, time_slot, week_number, start_date, end_date)
values (1, 'PM', 1, current_date, null);

insert into exclusion_days_of_week(exclusion_id, day_of_week) select 2, 'MONDAY' where extract(isodow from current_date) = 1;
insert into exclusion_days_of_week(exclusion_id, day_of_week) select 2, 'TUESDAY' where extract(isodow from current_date) = 2;
insert into exclusion_days_of_week(exclusion_id, day_of_week) select 2, 'WEDNESDAY' where extract(isodow from current_date) = 3;
insert into exclusion_days_of_week(exclusion_id, day_of_week) select 2, 'THURSDAY' where extract(isodow from current_date) = 4;
insert into exclusion_days_of_week(exclusion_id, day_of_week) select 2, 'FRIDAY' where extract(isodow from current_date) = 5;
insert into exclusion_days_of_week(exclusion_id, day_of_week) select 2, 'SATURDAY' where extract(isodow from current_date) = 6;
insert into exclusion_days_of_week(exclusion_id, day_of_week) select 2, 'SUNDAY' where extract(isodow from current_date) = 7;


insert into exclusion(allocation_id, time_slot, week_number, start_date, end_date)
values (1, 'ED', 1, current_date - interval '7 day' , null);

insert into exclusion_days_of_week(exclusion_id, day_of_week) select 3, 'MONDAY' where extract(isodow from current_date) = 1;
insert into exclusion_days_of_week(exclusion_id, day_of_week) select 3, 'TUESDAY' where extract(isodow from current_date) = 2;
insert into exclusion_days_of_week(exclusion_id, day_of_week) select 3, 'WEDNESDAY' where extract(isodow from current_date) = 3;
insert into exclusion_days_of_week(exclusion_id, day_of_week) select 3, 'THURSDAY' where extract(isodow from current_date) = 4;
insert into exclusion_days_of_week(exclusion_id, day_of_week) select 3, 'FRIDAY' where extract(isodow from current_date) = 5;
insert into exclusion_days_of_week(exclusion_id, day_of_week) select 3, 'SATURDAY' where extract(isodow from current_date) = 6;
insert into exclusion_days_of_week(exclusion_id, day_of_week) select 3, 'SUNDAY' where extract(isodow from current_date) = 7;

insert into scheduled_instance(activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, comment, time_slot)
values (1, current_timestamp, '10:00:00', '10:00:00', false, null, null, null, null, 'AM');

insert into scheduled_instance(activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, comment, time_slot)
values (1, current_timestamp, '12:00:00', '13:00:00', false, null, null, null, null, 'PM');

insert into scheduled_instance(activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, comment, time_slot)
values (1, current_timestamp, '18:00:00', '19:00:00', false, null, null, null, null, 'ED');





