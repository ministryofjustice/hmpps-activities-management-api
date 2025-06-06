--
-- The time slots for the prison have been set at two extremes very early or very late to prevent test fragility with timings
--
INSERT INTO prison_regime (prison_regime_id, prison_code, am_start, am_finish, pm_start, pm_finish, ed_start, ed_finish)
VALUES(3, 'PVI', '00:01:00', '00:02:00', '13:00:00', '16:30:00', '23:58:00', '23:59:00');

insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, created_time, created_by, paid)
values (1, 'PVI', 1, 1, true, false, false, false, 'H', 'Maths', 'Maths Level 1', '2022-10-10', null, 'high', '2022-9-21 00:00:00', 'SEED USER', true);

insert into activity_pay(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
values (1, 1, 'BAS', 'Basic', 1, 125, 150, 1);

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (1, 1, 'Maths AM', 1, 'L1', 'Location 1', 10, '2022-10-10');

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag, tuesday_flag, wednesday_flag, thursday_flag, friday_flag, saturday_flag, sunday_flag, time_slot)
values (1, 1, '00:00:00', '00:01:00', true, true, true, true, true, true, true, 'AM');

insert into scheduled_instance(activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, comment, time_slot)
values (1, current_date, '00:00:00', '00:01:00', false, null, null, null, null, 'AM');

insert into scheduled_instance(activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, comment, time_slot)
values (1, current_date + 1, '00:00:00', '00:01:00', false, null, null, null, null, 'AM');

insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, created_time, created_by, paid)
values (2, 'PVI', 1, 1, true, false, false, false, 'H', 'English', 'English Level 1', '2022-10-10', null, 'high', '2022-9-21 00:00:00', 'SEED USER', true);

insert into activity_pay(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
values (2, 2, 'BAS', 'Basic', 1, 125, 150, 1);

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (2, 2, 'English AM', 1, 'L1', 'Location 1', 10, '2022-10-10');

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag, tuesday_flag, wednesday_flag, thursday_flag, friday_flag, saturday_flag, sunday_flag, time_slot)
values (2, 2, '00:00:00', '00:01:00', true, true, true, true, true, true, true, 'AM');

insert into scheduled_instance(activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, comment, time_slot)
values (2, current_date, '00:00:00', '00:01:00', false, null, null, null, null, 'AM');

insert into scheduled_instance(activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, comment, time_slot)
values (2, current_date + 1, '00:00:00', '00:01:00', false, null, null, null, null,'AM');

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (1, 1, 'A11111A', 10001, 1, '2022-10-10', null, '2022-10-10 09:00:00', 'MR BLOGS', null, null, null, null, null, null, 'SUSPENDED_WITH_PAY');

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (2, 2, 'A11111A', 10001, 1, '2022-10-10', null, '2022-10-10 09:00:00', 'MR BLOGS', null, null, null, null, null, null, 'SUSPENDED_WITH_PAY');

insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, issue_payment)
values (1, 1, 'A11111A', 7, null, current_timestamp, 'Activities Management Service', 'COMPLETED', true);

insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, issue_payment)
values (2, 2, 'A11111A', 7, null, current_timestamp, 'Activities Management Service', 'COMPLETED', false);

insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, issue_payment)
values (3, 3, 'A11111A', 7, null, current_timestamp, 'Activities Management Service', 'COMPLETED', true);

insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, issue_payment)
values (4, 4, 'A11111A', 7, null, current_timestamp, 'Activities Management Service', 'COMPLETED', false);

insert into planned_suspension(planned_suspension_id, allocation_id, planned_start_date, planned_end_date, planned_by, planned_at, updated_by, updated_at, paid)
values (1, 1, current_date - 2, current_date + 2, 'MRS BLOGS', current_timestamp, 'MRS BLOGS', current_timestamp, true);

insert into planned_suspension(planned_suspension_id, allocation_id, planned_start_date, planned_end_date, planned_by, planned_at, updated_by, updated_at, paid)
values (2, 2, current_date - 2, current_date + 2, 'MRS BLOGS', current_timestamp, 'MRS BLOGS', current_timestamp, true);
