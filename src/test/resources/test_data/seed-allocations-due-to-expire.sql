--
-- Test data for activity offender deallocation for an activity with no end date. Only one of the allocations should be ended and one allocation expired
--
INSERT INTO prison_regime (prison_code, am_start, am_finish, pm_start, pm_finish, ed_start, ed_finish)
VALUES('PVI', '10:00:00', '11:00:00', '13:00:00', '16:30:00', '18:00:00', '20:00:00');

insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, created_time, created_by, paid)
values (1, 'PVI', 1, 1, true, false, false, false, 'H', 'Maths', 'Maths Level 1', current_timestamp - interval '10 day', null, 'high', '2022-9-21 00:00:00', 'SEED USER', true);

insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, created_time, created_by, paid)
values (2, 'PVI', 1, 1, true, false, false, false, 'H', 'English', 'English Level 1', current_timestamp - interval '10 day', null, 'high', '2022-9-21 00:00:00', 'SEED USER', true);

insert into activity_pay(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
values (1, 1, 'BAS', 'Basic', 1, 125, 150, 1);

insert into activity_minimum_education_level(activity_minimum_education_level_id, activity_id, education_level_code, education_level_description, study_area_code, study_area_description)
values (1, 1, '1', 'Reading Measure 1.0', 'ENGLA', 'English Language');

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date, end_date)
values (1, 1, 'Maths AM', 1, 'L1', 'Location 1', 10, current_timestamp - interval '1 day', null);

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date, end_date)
values (2, 2, 'English AM', 1, 'L1', 'Location 1', 10, current_timestamp - interval '1 day', null);

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (1, 1, 'A11111A', 10001, 1, current_timestamp + interval '10 day', null, '2022-10-10 09:00:00', 'MRS BLOGS', null, null, null, current_timestamp - interval '5 hour', 'Activities Management Service', 'Temporarily released from prison', 'AUTO_SUSPENDED');

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (2, 2, 'A11111A', 10001, 1, current_timestamp + interval '1 day', null, '2022-10-10 09:00:00', 'MRS BLOGS', null, null, null, null, null, null, 'PENDING');

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (3, 1, 'B11111B', 10002, 1, current_timestamp + interval '1 day', null, '2022-10-10 09:00:00', 'MRS BLOGS', null, null, null, null, null, null, 'PENDING');

insert into waiting_list (waiting_list_id, prison_code, prisoner_number, booking_id, application_date, activity_id, activity_schedule_id, requested_by, status, creation_time, created_by, comments, declined_reason, updated_time, updated_by, allocation_id)
values (1, 'PVI', 'A11111A', 10001, '2023-06-23', 2, 2, 'Fred Bloggs', 'PENDING', '2023-08-02 13:37:47.534000', 'test user', 'The prisoner has specifically requested to attend this activity', null, null, null, null);

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, time_slot)
values (1, 1, current_timestamp + interval '1 day', '09:00:00', '12:00:00', false, null, null, 'AM');

insert into advance_attendance(scheduled_instance_id, prisoner_number, issue_payment, recorded_time, recorded_by)
values (1, 'A11111A', true,'2024-01-23 12:00:00.000000', 'John Smith');

insert into advance_attendance(scheduled_instance_id, prisoner_number, issue_payment, recorded_time, recorded_by)
values (1, 'B11111B', true,'2024-01-23 12:00:00.000000', 'John Smith');
