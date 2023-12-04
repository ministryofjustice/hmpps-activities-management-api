--
-- Test data for scheduled events multiple activities.
--
insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, minimum_incentive_nomis_code, minimum_incentive_level, created_time, created_by, paid)
values (1, 'MDI', 1, 1, true, false, false, false, 'H', 'Maths', 'Maths Level 1', '2022-10-01', null, 'high', 'BAS', 'Basic', '2022-9-21 00:00:00', 'SEED USER', true);

insert into activity_pay(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
values (1, 1, 'BAS', 'Basic', 1, 125, 150, 1);

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date, end_date, schedule_weeks)
values (1, 1, 'Maths Level 1', 1, 'MDI-EDU-ROOM1', 'Education room 1', 10, '2022-10-01', null, 1);

insert into activity_schedule_slot(activity_schedule_id, start_time, end_time, monday_flag, tuesday_flag, wednesday_flag, thursday_flag, friday_flag, saturday_flag, sunday_flag, week_number)
values (1, '09:00:00', '12:00:00', true, true, true, true, true, true, true, 1),
       (1, '13:00:00', '16:30:00', true, true, true, true, true, true, true, 1);

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (1, 1, 'G4793VF', 10001, 1, '2022-10-01', null, '2022-10-01 09:00:00', 'MR BLOGS', null, null, null, null, null, null, 'ACTIVE');

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (2, 1, 'A5193DY', 10002, 2, '2022-10-01', null, '2022-10-01 09:00:00', 'MR BLOGS', null, null, null, null, null, null, 'ACTIVE');

insert into scheduled_instance(activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, comment)
values (1, current_date, '09:00:00', '12:00:00', false, null, null, null, null),
       (1, current_date, '13:00:00', '16:30:00', false, null, null, null, null),
       (1, current_date + interval '1' day, '09:00:00', '12:00:00', false, null, null, null, null),
       (1, current_date + interval '1' day, '13:00:00', '16:30:00', false, null, null, null, null);

insert into exclusion(allocation_id, monday_flag, tuesday_flag, wednesday_flag, thursday_flag, friday_flag, saturday_flag, sunday_flag, slot_start_time, week_number, start_date)
values (2, true, true, true, true, true, true, true, '09:00:00', 1, current_date + interval '1' day);
