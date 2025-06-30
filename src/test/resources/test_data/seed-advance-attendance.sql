INSERT INTO prison_regime (prison_regime_id, prison_code, am_start, am_finish, pm_start, pm_finish, ed_start, ed_finish)
VALUES(3, 'PVI', '10:00:00', '11:00:00', '13:00:00', '16:30:00', '18:00:00', '20:00:00');

insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, summary, start_date, risk_level, created_time, created_by, paid)
values (1, 'PVI', 1, 1, 'Maths', '2022-10-10', 'high', '2022-9-21 00:00:00', 'SEED USER', true);

insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, summary, start_date, risk_level, created_time, created_by, paid)
values (2, 'PVI', 1, 1, 'English', '2022-10-10', 'high', '2022-9-21 00:00:00', 'SEED USER', false);

insert into activity_pay(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
values (1, 1, 'STD', 'Standard', 1, 125, 150, 1);

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (1, 1, 'Maths AM', 1, 'L1', 'Location 1', 10, '2022-10-10');

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (2, 2, 'English PM', 1, 'L1', 'Location 1', 10, '2022-10-10');

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag, tuesday_flag, wednesday_flag, thursday_flag, friday_flag, saturday_flag, sunday_flag, time_slot)
values (1, 1, '10:00:00', '11:00:00', true, true, true, true, true, true, true, 'AM');

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag, tuesday_flag, wednesday_flag, thursday_flag, friday_flag, saturday_flag, sunday_flag, time_slot)
values (2, 2, '13:00:00', '14:00:00', true, true, true, true, true, true, true, 'AM');

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (1, 1, 'G4793VF', 10001, 1, '2022-10-10', null, '2022-10-10 09:00:00', 'MR BLOGS', null, null, null, null, null, null, 'ACTIVE');

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (2, 2, 'G4793VF', 10001, 1, '2022-10-10', null, '2022-10-10 09:00:00', 'MR BLOGS', null, null, null, null, null, null, 'ACTIVE');

insert into scheduled_instance(activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, comment, time_slot)
values (1, current_timestamp + interval '1 day', '10:00:00', '11:00:00', false, null, null, null, null, 'AM');

insert into scheduled_instance(activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, comment, time_slot)
values (2, current_timestamp + interval '1 day', '13:00:00', '14:00:00', false, null, null, null, null, 'AM');



