-- Test data for attendance sync with multiple overlapping allocations for the same prisoner/schedule
-- This scenario previously caused NonUniqueResultException in Hibernate 7.3
-- Real scenario: 4 prisoners each with 2 allocations (one ended same day, one still active)

insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, created_time, created_by, paid)
values (1, 'PVI', 1, 1, true, false, false, false, 'H', 'Maths', 'Maths Level 1', '2022-10-10', null, 'high', '2022-9-21 00:00:00', 'SEED USER', true);

insert into activity_pay(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
values (1, 1, 'BAS', 'Basic', 1, 125, 150, 1);

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (1, 1, 'Maths AM', 1, 'L1', 'Location 1', 10, '2022-10-10');

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag, time_slot)
values (1, 1, '10:00:00', '11:00:00', true, 'AM');

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (2, 1, 'Maths PM', 2, 'L2', 'Location 2', 10, '2022-10-10');

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag, time_slot)
values (2, 2, '14:00:00', '15:00:00', true, 'PM');

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (3, 1, 'Maths EVE', 3, 'L3', 'Location 3', 10, '2022-10-10');

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag, time_slot)
values (3, 3, '18:00:00', '19:00:00', true, 'ED');

-- Prisoner A (A11111A, booking 90001) on schedule 1: ended + active
insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (1, 1, 'A11111A', 90001, 1, '2026-07-20', '2026-07-20', '2026-07-20 09:00:00', 'MR BLOGS', '2026-07-20 09:00:00', 'SYSTEM', 'ENDED', null, null, null, 'ENDED');

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (2, 1, 'A11111A', 90001, 1, '2026-07-20', null, '2026-07-20 09:00:00', 'MR BLOGS', null, null, null, null, null, null, 'ACTIVE');

-- Prisoner B (A22222A, booking 90002) on schedule 2: ended + active
insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (3, 2, 'A22222A', 90002, 1, '2026-07-20', '2026-07-20', '2026-07-20 09:00:00', 'MR BLOGS', '2026-07-20 09:00:00', 'SYSTEM', 'ENDED', null, null, null, 'ENDED');

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (4, 2, 'A22222A', 90002, 1, '2026-07-20', null, '2026-07-20 09:00:00', 'MR BLOGS', null, null, null, null, null, null, 'ACTIVE');

-- Prisoner C (A33333A, booking 90003) on schedule 3: ended + active
insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (5, 3, 'A33333A', 90003, 1, '2026-07-20', '2026-07-20', '2026-07-20 09:00:00', 'MR BLOGS', '2026-07-20 09:00:00', 'SYSTEM', 'ENDED', null, null, null, 'ENDED');

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (6, 3, 'A33333A', 90003, 1, '2026-07-20', null, '2026-07-20 09:00:00', 'MR BLOGS', null, null, null, null, null, null, 'ACTIVE');

-- Prisoner D (A44444A, booking 90004) on schedule 3: ended + active
insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (7, 3, 'A44444A', 90004, 1, '2026-07-20', '2026-07-20', '2026-07-20 09:00:00', 'MR BLOGS', '2026-07-20 09:00:00', 'SYSTEM', 'ENDED', null, null, null, 'ENDED');

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (8, 3, 'A44444A', 90004, 1, '2026-07-20', null, '2026-07-20 09:00:00', 'MR BLOGS', null, null, null, null, null, null, 'ACTIVE');

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, comment, time_slot)
values (1, 1, '2026-07-20', '10:00:00', '11:00:00', false, null, null, null, null, 'AM');

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, comment, time_slot)
values (2, 2, '2026-07-20', '14:00:00', '15:00:00', false, null, null, null, null, 'PM');

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, comment, time_slot)
values (3, 3, '2026-07-20', '18:00:00', '19:00:00', false, null, null, null, null, 'ED');

-- Attendance records - one per prisoner
insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces, issue_payment)
values (1, 1, 'A11111A', 1, 'Test comment', '2026-07-20 10:00:00', 'Recorder', 'COMPLETED', 100, null, null, false);

insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces, issue_payment)
values (2, 2, 'A22222A', 1, 'Test comment', '2026-07-20 14:00:00', 'Recorder', 'COMPLETED', 100, null, null, false);

insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces, issue_payment)
values (3, 3, 'A33333A', 1, 'Test comment', '2026-07-20 18:00:00', 'Recorder', 'COMPLETED', 100, null, null, false);

insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces, issue_payment)
values (4, 3, 'A44444A', 1, 'Test comment', '2026-07-20 18:00:00', 'Recorder', 'COMPLETED', 100, null, null, false);
