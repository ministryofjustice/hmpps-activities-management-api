-- Two activities for prisoner A11111A at PVI:
--   Activity 1 (activity_id=1): internal (outside_work = false)
--   Activity 2 (activity_id=2): external (outside_work = true)
-- Each has one allocation and future attendance records.

INSERT INTO prison_regime (prison_regime_id, prison_code, am_start, am_finish, pm_start, pm_finish, ed_start, ed_finish)
VALUES (3, 'PVI', '00:01:00', '00:02:00', '13:00:00', '16:30:00', '23:58:00', '23:59:00');

-- Internal activity (outside_work = false)
INSERT INTO activity (activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, created_time, created_by, paid)
VALUES (1, 'PVI', 1, 1, true, false, false, false, 'H', 'Maths', 'Maths Level 1', '2022-10-10', null, 'high', '2022-9-21 00:00:00', 'SEED USER', true);

INSERT INTO activity_pay (activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
VALUES (1, 1, 'BAS', 'Basic', 1, 125, 150, 1);

INSERT INTO activity_minimum_education_level (activity_minimum_education_level_id, activity_id, education_level_code, education_level_description, study_area_code, study_area_description)
VALUES (1, 1, '1', 'Reading Measure 1.0', 'ENGLA', 'English Language');

INSERT INTO activity_schedule (activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date)
VALUES (1, 1, 'Maths AM', 1, 'L1', 'Location 1', 10, '2022-10-10');

INSERT INTO activity_schedule_slot (activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag, tuesday_flag, wednesday_flag, thursday_flag, friday_flag, saturday_flag, sunday_flag, time_slot)
VALUES (1, 1, '00:01:00', '00:02:00', true, true, true, true, true, true, true, 'AM');

INSERT INTO allocation (allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
VALUES (1, 1, 'A11111A', 10001, 1, '2022-10-10', null, '2022-10-10 09:00:00', 'MR BLOGS', null, null, null, null, null, null, 'ACTIVE');

-- Past scheduled instance (attendance_id=1 - should not be touched)
INSERT INTO scheduled_instance (scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, comment, time_slot)
VALUES (1, 1, current_date, '00:01:00', '00:02:00', false, null, null, null, null, 'AM');

INSERT INTO attendance (attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces)
VALUES (1, 1, 'A11111A', null, null, null, null, 'WAITING', null, null, null);

-- Future scheduled instance for internal activity (attendance_id=2 - should be AUTO_SUSPENDED on non-ROTL, untouched on ROTL)
INSERT INTO scheduled_instance (scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, comment, time_slot)
VALUES (2, 1, current_date + 1, '00:01:00', '00:02:00', false, null, null, null, null, 'AM');

INSERT INTO attendance (attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces)
VALUES (2, 2, 'A11111A', null, null, null, null, 'WAITING', null, null, null);

-- External activity (outside_work = true)
INSERT INTO activity (activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, created_time, created_by, paid)
VALUES (2, 'PVI', 1, 1, true, false, false, true, 'H', 'Work ROTL', 'Outside Work', '2022-10-10', null, 'high', '2022-9-21 00:00:00', 'SEED USER', true);

INSERT INTO activity_pay (activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
VALUES (2, 2, 'BAS', 'Basic', 1, 125, 150, 1);

INSERT INTO activity_minimum_education_level (activity_minimum_education_level_id, activity_id, education_level_code, education_level_description, study_area_code, study_area_description)
VALUES (2, 2, '1', 'Reading Measure 1.0', 'ENGLA', 'English Language');

INSERT INTO activity_schedule (activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date)
VALUES (2, 2, 'Outside Work AM', 3, 'L3', 'Location 3', 10, '2022-10-10');

INSERT INTO activity_schedule_slot (activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag, tuesday_flag, wednesday_flag, thursday_flag, friday_flag, saturday_flag, sunday_flag, time_slot)
VALUES (2, 2, '00:01:00', '00:02:00', true, true, true, true, true, true, true, 'AM');

INSERT INTO allocation (allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status)
VALUES (2, 2, 'A11111A', 10001, 1, '2022-10-10', null, '2022-10-10 09:00:00', 'MR BLOGS', null, null, null, null, null, null, 'ACTIVE');

-- Future scheduled instance for external activity (attendance_id=3 - should be untouched on ROTL, AUTO_SUSPENDED otherwise)
INSERT INTO scheduled_instance (scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, comment, time_slot)
VALUES (3, 2, current_date + 1, '00:01:00', '00:02:00', false, null, null, null, null, 'AM');

INSERT INTO attendance (attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces)
VALUES (3, 3, 'A11111A', null, null, null, null, 'WAITING', null, null, null);
