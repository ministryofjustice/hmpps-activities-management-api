insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, summary, start_date, risk_level, created_time, created_by, paid, in_cell)
values (1, 'RSI', 1, 1, 'Maths', '2022-10-10', 'high', '2022-9-21 00:00:00', 'BLOGGSJ', false, true);

insert into activity_schedule(activity_schedule_id, activity_id, description, capacity, start_date, schedule_weeks)
values (1, 1, 'Maths', 10, '2022-10-10', 2);

insert into activity_schedule_slot(activity_schedule_id, week_number, time_slot, start_time, end_time, monday_flag, tuesday_flag, wednesday_flag, thursday_flag, friday_flag)
values (1, 1, 'AM', '08:00:00', '09:00:00', true, true, true, false, false);

insert into activity_schedule_slot(activity_schedule_id, week_number, time_slot, start_time, end_time, monday_flag, tuesday_flag, wednesday_flag, thursday_flag, friday_flag)
values (1, 1, 'PM', '13:00:00', '14:00:00', true, true, true, false, false);

insert into activity_schedule_slot(activity_schedule_id, week_number, time_slot, start_time, end_time, monday_flag, tuesday_flag, wednesday_flag, thursday_flag, friday_flag)
values (1, 1, 'ED', '17:00:00', '18:00:00', true, true, true, false, false);

insert into activity_schedule_slot(activity_schedule_id, week_number, time_slot, start_time, end_time, monday_flag, tuesday_flag, wednesday_flag, thursday_flag, friday_flag)
values (1, 2, 'AM', '08:00:00', '09:00:00', false, false, false, true, true);

insert into activity_schedule_slot(activity_schedule_id, week_number, time_slot, start_time, end_time, monday_flag, tuesday_flag, wednesday_flag, thursday_flag, friday_flag)
values (1, 2, 'PM', '13:00:00', '14:00:00', false, false, false, true, true);

insert into activity_schedule_slot(activity_schedule_id, week_number, time_slot, start_time, end_time, monday_flag, tuesday_flag, wednesday_flag, thursday_flag, friday_flag)
values (1, 2, 'ED', '17:00:00', '18:00:00', false, false, false, true, true);

insert into allocation(activity_schedule_id, prisoner_number, booking_id, start_date, allocated_time, allocated_by, prisoner_status)
values (1, 'A11111A', 10001, '2022-10-10', '2022-10-10 09:00:00', 'MR BLOGGS', 'ACTIVE');

insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, summary, start_date, risk_level, created_time, created_by, paid, in_cell)
values (2, 'RSI', 1, 1, 'English', '2022-10-10', 'high', '2022-9-21 00:00:00', 'BLOGGSJ', false, true);

insert into activity_schedule(activity_schedule_id, activity_id, description, capacity, start_date, schedule_weeks)
values (2, 2, 'English', 10, '2022-10-10', 1);

insert into activity_schedule_slot(activity_schedule_id, week_number, time_slot, start_time, end_time, monday_flag, tuesday_flag, wednesday_flag, thursday_flag, friday_flag)
values (2, 1, 'AM', '08:00:00', '09:00:00', true, true, false, false, false);

insert into allocation(activity_schedule_id, prisoner_number, booking_id, start_date, allocated_time, allocated_by, prisoner_status)
values (2, 'A11111A', 10001, '2022-10-10', '2022-10-10 09:00:00', 'MR BLOGGS', 'ACTIVE');

