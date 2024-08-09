--
-- Test data for activity pending allocation activation.
--
INSERT INTO prison_regime (prison_code, am_start, am_finish, pm_start, pm_finish, ed_start, ed_finish)
VALUES('PVI', '10:00:00', '11:00:00', '13:00:00', '16:30:00', '18:00:00', '20:00:00');

insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, created_time, created_by, paid)
values (1, 'PVI', 1, 1, true, false, false, false, 'H', 'Maths Level 1', 'Maths Level 1', '2020-01-01', null, 'high', '2022-9-21 00:00:00', 'SEED USER', true);

insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, created_time, created_by, paid)
values (2, 'PVI', 1, 1, true, false, false, false, 'H', 'Activity Summary WL', 'Activity Summary WL', '2020-01-01', null, 'high', '2022-9-21 00:00:00', 'SEED USER', true);

insert into activity_pay(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
values (1, 1, 'BAS', 'Basic', 1, 125, 150, 1);

insert into activity_minimum_education_level(activity_minimum_education_level_id, activity_id, education_level_code, education_level_description, study_area_code, study_area_description)
values (1, 1, '1', 'Reading Measure 1.0', 'ENGLA', 'English Language');

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date, end_date)
values (1, 1, 'Maths AM', 1, 'L1', 'Location 1', 10, '2020-01-01', null);

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date, end_date)
values (2, 2, 'Maths AM', 1, 'L1', 'Location 1', 10, '2020-01-01', null);

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (1, 1, '111111', 10001, 1, '2020-01-02', '2020-12-01', '2020-01-01 09:00:00', 'MRS BLOGS', null, null, null, null, null, null, 'ENDED');

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (2, 1, '111111', 10001, 1, current_date, null, '2022-10-10 09:00:00', 'MRS BLOGS', null, null, null, null, null, null, 'ACTIVE');

insert into allocation(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
values (3, 2, '111222', 10001, 1, '2022-10-10', null, '2022-10-10 09:00:00', 'MRS BLOGS', null, null, null, null, null, null, 'ACTIVE');

insert into waiting_list(waiting_list_id, prison_code, prisoner_number, booking_id, application_date, activity_id, activity_schedule_id, requested_by, status, creation_time, created_by)
values (2, 'PVI', '111222', 10001, '2023-08-08', 2, 1, 'Prison staff', 'APPROVED', '2022-10-10 09:00:00', 'SEED USER');

insert into waiting_list(waiting_list_id, prison_code, prisoner_number, booking_id, application_date, activity_id, activity_schedule_id, requested_by, status, creation_time, created_by, status_updated_time, comments)
values (3, 'PVI', '111222', 10001, '2023-08-08', 2, 1, 'Prison staff', 'APPROVED', '2022-10-12 09:00:00', 'SEED USER', '2022-11-12 09:00:00', 'added to the waiting list');

insert into appointment_series_schedule (appointment_series_schedule_id, frequency, number_of_appointments)
VALUES (1, 'DAILY', 3);

insert into appointment_series (appointment_series_id, appointment_type, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, appointment_series_schedule_id, extra_information, created_time, created_by)
VALUES (1, 'GROUP', 'PVI', 'EDUC', 1, 123, false, '2022-10-12', '09:00:00', '10:30:00', 1, 'Prayer session', (now()::date - 2)::timestamp, 'TEST.USER');

insert into appointment_series (appointment_series_id, appointment_type, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, appointment_series_schedule_id, created_time, created_by)
VALUES (2, 'GROUP', 'PVI', 'EDUC', 1, 123, false, '2022-10-12', '09:00:00', '10:30:00', 1, (now()::date - 2)::timestamp, 'TEST.USER');

insert into appointment_series (appointment_series_id, appointment_type, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, appointment_series_schedule_id, created_time, created_by)
VALUES (3, 'GROUP', 'PVI', 'EDUC', 1, 123, false, '2022-10-12', '09:00:00', '10:30:00', 1, (now()::date - 2)::timestamp, 'TEST.USER');

insert into appointment (appointment_id, appointment_series_id, sequence_number, prison_code, category_code, appointment_tier_id, internal_location_id, in_cell, start_date, start_time, end_time, created_time, created_by)
VALUES  (1, 1, 1, 'PVI', 'EDUC', 1, 123, false, '2022-10-12', '09:30:00', '11:45:00', '2022-10-11 09:00:00', 'TEST.USER'),
        (2, 2, 2, 'PVI', 'EDUC', 1, 123, false, '2022-10-13', '14:00:00', '15:30:00', '2022-10-08 09:00:00', 'TEST.USER'),
        (3, 3, 3, 'PVI', 'EDUC', 1, 123, false, '2022-10-14', '06:00:00', '08:30:00', '2022-10-09 09:00:00', 'TEST.USER');

insert into appointment_attendee (appointment_attendee_id, appointment_id, prisoner_number, booking_id, attended, attendance_recorded_time, attendance_recorded_by)
VALUES  (1, 1, '111222', 1, null, null, null),
        (2, 2, '111222', 2, true, '2022-10-12 09:00:00', 'PREV.ATTENDANCE.RECORDED.BY'),
        (3, 3, '111222', 3, false, '2022-10-12 09:00:00', 'PREV.ATTENDANCE.RECORDED.BY');

--attendance
insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, created_time, created_by, paid)
values (3, 'PVI', 3, 1, true, true, false, false, 'H', 'QAtestingKitchenActivity', 'QAtestingKitchenActivity', '2023-07-21', '2024-01-02', 'high', '2023-07-20 10:06:22.993053', 'SEED USER', true),
       (6, 'PVI', 5, 1, true, false, false, false, 'H', 'Adams Advanced Archery', 'Adams Advanced Archery', '2023-07-21', '5000-10-02', 'high', '2023-07-20 16:05:16.762526', 'SEED USER', true);

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date, end_date)
values (3, 3, 'QAtestingKitchenActivity', 14435, 'A3EDU', 'A3 EDUCATION', 150, '2023-07-21', '2024-01-02'),
       (6, 6, 'Adams Advanced Archery', 14441, 'BWINT1', 'AB WING INTERVIEW 1', 102, '2023-07-21', '5000-10-02');

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, comment, time_slot)
values (14, 3, '2023-07-21', '09:00:00', '12:00:00', false, null, null, null, null, 'AM'),
       (15, 3, '2023-07-21', '13:00:00', '16:30:00', false, null, null, null, null, 'PM'),
       (16, 3, '2023-07-21', '18:00:00', '20:00:00', false, null, null, null, null, 'ED'),
       (18, 3, '2023-07-22', '13:00:00', '16:30:00', false, null, null, null, null, 'PM'),
       (19, 3, '2023-07-22', '18:00:00', '20:00:00', false, null, null, null, null, 'ED'),
       (71, 6, '2023-07-20', '09:00:00', '12:00:00', true, '2023-07-21 17:37:52.000000', 'SCH_ACTIVITY', 'Staff unavailable', null, 'AM'),
       (72, 6, '2023-07-21', '13:00:00', '16:30:00', false, null, null, null, null, 'PM');

insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces)
values (9, 14, 'A4743DZ', 9, null, '2023-07-21 16:55:34.057591', 'MR BLOGS', 'COMPLETED', null, null, null),
       (14, 15, 'A4743DZ', 9, null, '2023-07-21 16:56:52.200196', 'MR BLOGS', 'COMPLETED', null, null, null),
       (19, 16, 'A4743DZ', 7, null, '2023-07-21 16:58:21.231139', 'MR BLOGS', 'COMPLETED', null, null, null),
       (60, 18, 'A4745DZ', 9, null, '2023-07-24 17:51:29.304927', 'MR BLOGS', 'COMPLETED', null, null, null),
       (74, 19, 'A4745DZ', 9, null, null, null, 'WAITING', null, null, null),
       (26, 71, 'G9372GQ', 9, null, '2023-07-21 15:51:39.824964', 'MR BLOGS', 'COMPLETED', null, null, null),
       (30, 72, 'G9372GQ', 8, null, '2023-07-21 17:37:52.403630', 'MR BLOGS', 'COMPLETED', null, null, null);
