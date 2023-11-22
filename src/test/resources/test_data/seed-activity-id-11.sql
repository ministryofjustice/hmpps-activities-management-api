--
-- Test data for activity offender deallocation for an activity ending today.  Note one allocation is already ended, 3 are not.  This is to ensure it does not try end it again.
--
insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, minimum_incentive_nomis_code, minimum_incentive_level, created_time, created_by, paid)
values (1, 'PVI', 1, 1, true, false, false, false, 'H', 'Maths', 'Maths Level 1', current_date -1, current_date, 'high', 'BAS', 'Basic', '2022-9-21 00:00:00', 'SEED USER', true);

insert into activity_pay(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
values (1, 1, 'BAS', 'Basic', 1, 125, 150, 1);

insert into activity_minimum_education_level(activity_minimum_education_level_id, activity_id, education_level_code, education_level_description, study_area_code, study_area_description)
values (1, 1, '1', 'Reading Measure 1.0', 'ENGLA', 'English Language');

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date, end_date)
values (1, 1, 'Maths AM', 1, 'L1', 'Location 1', 10, current_date - 1, current_date);

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (1, 1, 'A11111A', 10001, 1, current_date - 1, null, '2022-10-10 09:00:00', 'MR BLOGS', null, null, null, null, null, null, 'ACTIVE');

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (2, 1, 'A22222A', 10002, 2, current_date - 1, current_date, '2022-10-10 09:00:00', 'MRS BLOGS', null, null, null, null, null, null, 'ACTIVE');

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (3, 1, 'A33333A', 10003, 2, current_date, current_date + 1, '2022-10-10 09:00:00', 'MRS BLOGS', null, null, null, null, null, null, 'ACTIVE');

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (4, 1, 'A33333A', 10004, 2, current_date - 1, current_date - 1, '2022-10-10 09:00:00', 'MRS BLOGS', current_timestamp - 1, 'Activities Management Service', 'ENDED', null, null, null, 'ENDED');

insert into waiting_list (waiting_list_id, prison_code, prisoner_number, booking_id, application_date, activity_id, activity_schedule_id, requested_by, status, creation_time, created_by, comments, declined_reason, updated_time, updated_by, allocation_id)
values (1, 'PVI', 'A11111A', 111111, '2023-06-23', 1, 1, 'Fred Bloggs', 'PENDING', '2023-08-02 13:37:47.534000', 'test user', 'The prisoner has specifically requested to attend this activity', null, null, null, null);

insert into waiting_list (waiting_list_id, prison_code, prisoner_number, booking_id, application_date, activity_id, activity_schedule_id, requested_by, status, creation_time, created_by, comments, declined_reason, updated_time, updated_by, allocation_id)
values (2, 'PVI', 'A22222A', 222222, '2023-06-23', 1, 1, 'Fred Bloggs', 'APPROVED', '2023-08-02 13:37:47.534000', 'test user', 'The prisoner has specifically requested to attend this activity', null, null, null, null);

insert into waiting_list (waiting_list_id, prison_code, prisoner_number, booking_id, application_date, activity_id, activity_schedule_id, requested_by, status, creation_time, created_by, comments, declined_reason, updated_time, updated_by, allocation_id)
values (3, 'PVI', 'A33333A', 333333, '2023-06-23', 1, 1, 'Fred Bloggs', 'PENDING', '2023-08-02 13:37:47.534000', 'test user', 'The prisoner has specifically requested to attend this activity', null, null, null, null);
