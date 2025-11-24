insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, created_time, created_by, paid)
values (1, 'MDI', 2, 2, true, false, false, false, 'H', 'Geography', 'Geography Level 1', '2022-10-01', null, 'high', '2022-9-21 00:00:00', 'SEED USER', false);

insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, created_time, created_by, paid)
values (2, 'MDI', 2, 2, true, false, false, false, 'H', 'Maths', 'Maths Level 1', '2022-10-01', null, 'high', '2022-9-21 00:00:00', 'SEED USER', false);

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, dps_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (1, 1, 'Geography AM', 1, '11111111-1111-1111-1111-111111111111', 'L1', 'Location MDI 1', 10, '2022-10-01');

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, dps_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (2, 2, 'Maths AM', 2, '22222222-2222-2222-2222-222222222222', 'L2', 'Location MDI 2', 10, '2022-10-01');

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag, time_slot)
values (1, 1, '09:00:00', '12:00:00', true, 'AM');

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag, time_slot)
values (2, 2, '09:00:00', '12:00:00', true, 'AM');

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (1, 1, 'A11111A', 10001, 1, '2022-10-01', null, '2022-10-01 10:00:00', 'MR BLOGS', null, null, null, null, null, null, 'ACTIVE');

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (2, 1, 'B22222B', 10002, 1, '2022-10-01', null, '2022-10-01 10:00:00', 'MR BLOGS', null, null, null, null, null, null, 'ACTIVE');

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (3, 2, 'B22222B', 10003, 1, '2022-10-01', null, '2022-10-01 10:00:00', 'MR BLOGS', null, null, null, null, null, null, 'ACTIVE');

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (4, 1, 'C33333C', 10001, 1, current_date + interval '1 day', null, '2022-10-01 10:00:00', 'MR BLOGS', null, null, null, null, null, null, 'ACTIVE');

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, time_slot)
values (1, 1, current_timestamp + interval '1 day', '09:00:00', '12:00:00', false, null, null, 'AM');

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, time_slot)
values (2, 2, current_timestamp + interval '1 day', '09:00:00', '12:00:00', false, null, null, 'AM');

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, time_slot)
values (3, 1, current_timestamp + interval '2 day', '09:00:00', '12:00:00', false, null, null, 'AM');

insert into advance_attendance(scheduled_instance_id, prisoner_number, issue_payment, recorded_time, recorded_by)
values (1, 'A11111A', true,'2024-01-23 12:00:00.000000', 'John Smith');

insert into advance_attendance(scheduled_instance_id, prisoner_number, issue_payment, recorded_time, recorded_by)
values (2, 'B22222B', true,'2024-01-23 12:00:00.000000', 'John Smith');

insert into advance_attendance(scheduled_instance_id, prisoner_number, issue_payment, recorded_time, recorded_by)
values (1, 'C33333C', true,'2024-01-23 12:00:00.000000', 'John Smith');

insert into advance_attendance(scheduled_instance_id, prisoner_number, issue_payment, recorded_time, recorded_by)
values (3, 'C33333C', true,'2024-01-23 12:00:00.000000', 'John Smith');


