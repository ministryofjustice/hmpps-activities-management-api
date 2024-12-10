-- Simulates migrated date where on_wing is true and internal_location_id is set.

insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, created_time, created_by, paid, on_wing)
values (100, 'MDI', 2, 2, true, false, false, false, 'H', 'History', 'History Level 1', '2022-10-01', null, 'high', '2022-9-21 00:00:00', 'SEED USER', false, true);

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (100, 100, 'Geography AM', 4, 'L1', 'History MDI 1', 10, '2022-10-01');

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag, time_slot)
values (100, 100, '09:00:00', '12:00:00', true, 'AM');

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (100, 100, 'A11111A', 10101, 1, '2022-10-01', null, '2022-10-01 10:00:00', 'MR BLOGS', null, null, null, null, null, null, 'ACTIVE');

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, time_slot)
values (100, 100, '2022-10-01', '09:00:00', '12:00:00', false, null, null, 'AM');

