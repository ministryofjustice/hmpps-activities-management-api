insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, summary, description, start_date, end_date, created_time, created_by)
values (1, 'MDI', 2, 2, true, 'Geography', 'Geography Level 1', '2022-10-01', null, '2022-9-21 00:00:00', 'SEED USER');

insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, summary, description, start_date, end_date, created_time, created_by)
values (2, 'MDI', 2, 2, true, 'English', 'English Level 2', '2022-11-01', null, '2022-9-21 00:00:00', 'SEED USER');

insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, summary, description, start_date, end_date, created_time, created_by)
values (3, 'PVI', 1, 1, true, 'Maths', 'Maths Level 1', '2022-10-10', null, '2022-9-21 00:00:00', 'SEED USER');

insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, summary, description, start_date, end_date, created_time, created_by)
values (4, 'PVI', 2, 2, true, 'English', 'English Level 2', '2022-10-21', null, '2022-9-21 00:00:00', 'SEED USER');

insert into activity_pay(activity_pay_id, activity_id, incentive_level, pay_band, rate, piece_rate, piece_rate_items)
values (1, 1, 'BAS', 'A', 101, 0, 0);

insert into activity_pay(activity_pay_id, activity_id, incentive_level, pay_band, rate, piece_rate, piece_rate_items)
values (2, 2, 'STD', 'B', 102, 0, 0);

insert into activity_pay(activity_pay_id, activity_id, incentive_level, pay_band, rate, piece_rate, piece_rate_items)
values (3, 3, 'ENH', 'C', 103, 0, 0);

insert into activity_pay(activity_pay_id, activity_id, incentive_level, pay_band, rate, piece_rate, piece_rate_items)
values (4, 4, 'GOLD', 'D', 104, 0, 0);

insert into activity_schedule(activity_schedule_id, activity_id, description, start_time, end_time, internal_location_id, internal_location_code, internal_location_description, capacity, monday_flag)
values (1, 1, 'Geography AM', '10:01:00', '11:00:00', 1, 'L1', 'Location MDI 1', 10, true);

insert into activity_schedule(activity_schedule_id, activity_id, description, start_time, end_time, internal_location_id, internal_location_code, internal_location_description, capacity, monday_flag)
values (2, 1, 'Geography PM', '14:01:00', '15:00:00', 2, 'L2', 'Location MDI 2', 10, true);

insert into activity_schedule(activity_schedule_id, activity_id, description, start_time, end_time, internal_location_id, internal_location_code, internal_location_description, capacity, monday_flag)
values (3, 2, 'English AM', '10:01:00', '11:00:00', 2, 'L2', 'Location MDI 2', 10, true);

insert into activity_schedule(activity_schedule_id, activity_id, description, start_time, end_time, internal_location_id, internal_location_code, internal_location_description, capacity, monday_flag)
values (4, 2, 'English PM', '14:01:00', '15:00:00', 1, 'L1', 'Location MDI 1', 10, true);

insert into activity_schedule(activity_schedule_id, activity_id, description, start_time, end_time, internal_location_id, internal_location_code, internal_location_description, capacity, monday_flag)
values (5, 3, 'Maths AM', '10:00:00', '11:00:00', 3, 'L1', 'Location PVI 1', 10, true);

insert into activity_schedule(activity_schedule_id, activity_id, description, start_time, end_time, internal_location_id, internal_location_code, internal_location_description, capacity, monday_flag)
values (6, 3, 'Maths PM', '14:00:00', '15:00:00', 4, 'L2', 'Location PVI 2', 10, true);

insert into activity_schedule(activity_schedule_id, activity_id, description, start_time, end_time, internal_location_id, internal_location_code, internal_location_description, capacity, monday_flag)
values (7, 4, 'English AM', '10:00:00', '11:00:00', 4, 'L2', 'Location PVI 2', 10, true);

insert into activity_schedule(activity_schedule_id, activity_id, description, start_time, end_time, internal_location_id, internal_location_code, internal_location_description, capacity, monday_flag)
values (8, 4, 'English PM', '14:00:00', '15:00:00', 3, 'L1', 'Location PVI 1', 10, true);

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, incentive_level, pay_band, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason)
values (1, 1, 'A11111A', 'BAS', 'A', '2022-10-01', null, '2022-10-01 10:00:00', 'MR BLOGS', null, null, null);

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, incentive_level, pay_band, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason)
values (2, 1, 'A22222A', 'STD', 'B', '2022-10-02', null, '2022-10-02 10:00:00', 'MRS BLOGS', null, null, null);

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, incentive_level, pay_band, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason)
values (3, 2, 'A33333A', 'STD', 'C', '2022-10-01', null, '2022-10-10 10:00:00', 'MR BLOGS', null, null, null);

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, incentive_level, pay_band, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason)
values (4, 2, 'A44444A', 'STD', 'C', '2022-10-02', null, '2022-10-10 10:00:00', 'MR BLOGS', null, null, null);

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, incentive_level, pay_band, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason)
values (5, 3, 'A33333A', 'STD', 'C', '2022-11-01', null, '2022-10-10 10:00:00', 'MR BLOGS', null, null, null);

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, incentive_level, pay_band, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason)
values (6, 3, 'A44444A', 'STD', 'C', '2022-11-02', null, '2022-10-10 10:00:00', 'MR BLOGS', null, null, null);

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, incentive_level, pay_band, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason)
values (7, 4, 'A11111A', 'STD', 'C', '2022-11-01', null, '2022-10-10 10:00:00', 'MR BLOGS', null, null, null);

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, incentive_level, pay_band, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason)
values (8, 4, 'A22222A', 'STD', 'C', '2022-11-02', null, '2022-10-10 10:00:00', 'MR BLOGS', null, null, null);

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, incentive_level, pay_band, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason)
values (9, 5, 'B11111A', 'BAS', 'A', '2022-10-10', null, '2022-10-01 10:00:00', 'MR BLOGS', null, null, null);

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, incentive_level, pay_band, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason)
values (10, 5, 'B22222A', 'STD', 'B', '2022-10-11', null, '2022-10-02 10:00:00', 'MRS BLOGS', null, null, null);

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, incentive_level, pay_band, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason)
values (11, 6, 'B33333A', 'STD', 'C', '2022-10-10', null, '2022-10-10 10:00:00', 'MR BLOGS', null, null, null);

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, incentive_level, pay_band, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason)
values (12, 6, 'B44444A', 'STD', 'C', '2022-10-11', null, '2022-10-10 10:00:00', 'MR BLOGS', null, null, null);

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, incentive_level, pay_band, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason)
values (13, 7, 'B33333A', 'STD', 'C', '2022-10-21', null, '2022-10-10 10:00:00', 'MR BLOGS', null, null, null);

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, incentive_level, pay_band, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason)
values (14, 7, 'B44444A', 'STD', 'C', '2022-10-22', null, '2022-10-10 10:00:00', 'MR BLOGS', null, null, null);

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, incentive_level, pay_band, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason)
values (15, 8, 'B11111A', 'STD', 'C', '2022-11-21', null, '2022-10-10 10:00:00', 'MR BLOGS', null, null, null);

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, incentive_level, pay_band, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason)
values (16, 8, 'B22222A', 'STD', 'C', '2022-11-22', null, '2022-10-10 10:00:00', 'MR BLOGS', null, null, null);

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by)
values (1, 1, '2022-10-01', '10:01:00', '11:00:00', false, null, null);

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by)
values (2, 1, '2022-10-02', '10:01:00', '11:00:00', false, null, null);

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by)
values (3, 1, '2022-10-03', '10:01:00', '11:00:00', false, null, null);

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by)
values (4, 1, '2022-10-04', '10:01:00', '11:00:00', false, null, null);

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by)
values (5, 1, '2022-10-05', '10:01:00', '11:00:00', false, null, null);

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by)
values (6, 2, '2022-10-01', '14:01:00', '15:00:00', false, null, null);

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by)
values (7, 2, '2022-10-02', '14:01:00', '15:00:00', false, null, null);

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by)
values (8, 2, '2022-10-03', '14:01:00', '15:00:00', false, null, null);

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by)
values (9, 2, '2022-10-04', '14:01:00', '15:00:00', false, null, null);

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by)
values (10, 2, '2022-10-05', '14:01:00', '15:00:00', false, null, null);

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by)
values (11, 3, '2022-11-01', '10:01:00', '11:00:00', false, null, null);

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by)
values (12, 3, '2022-11-02', '10:01:00', '11:00:00', false, null, null);

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by)
values (13, 3, '2022-11-03', '10:01:00', '11:00:00', false, null, null);

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by)
values (14, 3, '2022-11-04', '10:01:00', '11:00:00', false, null, null);

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by)
values (15, 3, '2022-11-05', '10:01:00', '11:00:00', false, null, null);

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by)
values (16, 4, '2022-11-01', '14:01:00', '15:00:00', false, null, null);

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by)
values (17, 4, '2022-11-02', '14:01:00', '15:00:00', false, null, null);

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by)
values (18, 4, '2022-11-03', '14:01:00', '15:00:00', false, null, null);

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by)
values (19, 4, '2022-11-04', '14:01:00', '15:00:00', false, null, null);

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by)
values (20, 4, '2022-11-05', '14:01:00', '15:00:00', false, null, null);

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by)
values (21, 5, '2022-10-10', '10:00:00', '11:00:00', false, null, null);

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by)
values (22, 5, '2022-10-11', '10:00:00', '11:00:00', false, null, null);

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by)
values (23, 5, '2022-10-12', '10:00:00', '11:00:00', false, null, null);

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by)
values (24, 5, '2022-10-13', '10:00:00', '11:00:00', false, null, null);

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by)
values (25, 5, '2022-10-14', '10:00:00', '11:00:00', false, null, null);

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by)
values (26, 6, '2022-10-10', '14:00:00', '15:00:00', false, null, null);

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by)
values (27, 6, '2022-10-11', '14:00:00', '15:00:00', false, null, null);

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by)
values (28, 6, '2022-10-12', '14:00:00', '15:00:00', false, null, null);

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by)
values (29, 6, '2022-10-13', '14:00:00', '15:00:00', false, null, null);

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by)
values (30, 6, '2022-10-14', '14:00:00', '15:00:00', false, null, null);

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by)
values (31, 7, '2022-10-21', '10:00:00', '11:00:00', false, null, null);

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by)
values (32, 7, '2022-10-22', '10:00:00', '11:00:00', false, null, null);

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by)
values (33, 7, '2022-10-23', '10:00:00', '11:00:00', false, null, null);

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by)
values (34, 7, '2022-10-24', '10:00:00', '11:00:00', false, null, null);

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by)
values (35, 7, '2022-10-25', '10:00:00', '11:00:00', false, null, null);

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by)
values (36, 8, '2022-10-21', '14:00:00', '15:00:00', false, null, null);

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by)
values (37, 8, '2022-10-22', '14:00:00', '15:00:00', false, null, null);

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by)
values (38, 8, '2022-10-23', '14:00:00', '15:00:00', false, null, null);

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by)
values (39, 8, '2022-10-24', '14:00:00', '15:00:00', false, null, null);

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by)
values (40, 8, '2022-11-25', '14:00:00', '15:00:00', false, null, null);

insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, posted, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces)
values (1, 1, 'A11111A', 3, null, false, null, null, 'SCH', null, null, null);

insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, posted, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces)
values (2, 1, 'A22222A', 3, null, false, null, null, 'SCH', null, null, null);

insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, posted, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces)
values (3, 2, 'A11111A', 1, null, false, null, null, 'SCH', null, null, null);

insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, posted, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces)
values (4, 2, 'A22222A', 3, null, false, null, null, 'SCH', null, null, null);

insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, posted, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces)
values (5, 3, 'A11111A', 3, null, false, null, null, 'SCH', null, null, null);

insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, posted, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces)
values (6, 3, 'A22222A', 1, null, false, null, null, 'SCH', null, null, null);

insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, posted, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces)
values (7, 4, 'A11111A', 4, null, false, null, null, 'SCH', null, null, null);

insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, posted, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces)
values (8, 4, 'A22222A', 4, null, false, null, null, 'SCH', null, null, null);

insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, posted, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces)
values (9, 5, 'A11111A', 3, null, false, null, null, 'SCH', null, null, null);

insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, posted, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces)
values (10, 5, 'A22222A', 3, null, false, null, null, 'SCH', null, null, null);