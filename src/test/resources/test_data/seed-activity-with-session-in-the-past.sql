--
-- Test data for scheduled events in the past test
--
INSERT INTO activity
(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, created_time, created_by, updated_time, updated_by, on_wing, off_wing, activity_organiser_id, paid)
VALUES(1, 'RSI', 1, 1, true, false, false, false, 'H', 'daf testing', 'daf testing', '2024-03-07', NULL, 'low', '2024-03-07 11:18:18.990', 'SCH_ACTIVITY', '2024-03-07 11:29:10.547', 'SCH_ACTIVITY', false, false, NULL, true);

INSERT INTO prison_pay_band
(prison_pay_band_id, display_sequence, nomis_pay_band, pay_band_alias, pay_band_description, prison_code)
VALUES(999, 89, 78, 'Low', 'Pay band 1 (Lowest)', 'RSI');

INSERT INTO activity_schedule
(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date, end_date, runs_on_bank_holiday, updated_time, updated_by, instances_last_updated_time, schedule_weeks)
VALUES(1, 1, 'daf testing', 67128, 'AWING', 'A WING', 3, '2024-03-07', NULL, false, '2024-03-07 11:29:10.547', 'SCH_ACTIVITY', '2024-03-07 11:25:15.652', 1);

INSERT INTO activity_schedule_slot
(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag, tuesday_flag, wednesday_flag, thursday_flag, friday_flag, saturday_flag, sunday_flag, week_number, time_slot)
VALUES(2, 1, '13:45:00', '16:45:00', true, true, true, true, true, true, true, 1, 'PM');
INSERT INTO activity_schedule_slot
(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag, tuesday_flag, wednesday_flag, thursday_flag, friday_flag, saturday_flag, sunday_flag, week_number, time_slot)
VALUES(3, 1, '17:30:00', '19:15:00', true, true, true, true, true, true, true, 1, 'ED');
INSERT INTO activity_schedule_slot
(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag, tuesday_flag, wednesday_flag, thursday_flag, friday_flag, saturday_flag, sunday_flag, week_number, time_slot)
VALUES(1, 1, '08:30:00', '11:45:00', true, true, true, true, true, true, true, 1, 'AM');

INSERT INTO allocation
(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status, planned_deallocation_id, deallocation_case_note_id)
VALUES(2, 1, 'G6268GL', 935073, 999, '2024-03-07', '2024-03-07', '2024-03-07 11:30:00.000', 'SCH_ACTIVITY', '2024-03-08 01:00:00.000', 'SCH_ACTIVITY', 'DISMISSED', '2024-03-07 11:30:54.521', 'Activities Management Service', 'Temporarily released or transferred', 'ENDED', NULL, NULL);

INSERT INTO planned_deallocation
(planned_deallocation_id, planned_date, planned_by, planned_reason, planned_at, allocation_id, case_note_id)
VALUES(1, '2023-08-31', 'SCH_ACTIVITY', 'PLANNED', '2023-07-19 19:26:31.816', 2, NULL);

UPDATE allocation set planned_deallocation_id = 1 where allocation_id = 2;

INSERT INTO allocation
(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status, planned_deallocation_id, deallocation_case_note_id)
VALUES(3, 1, 'G4206GA', 1033561, 999, '2024-03-07', NULL, '2024-03-07 13:25:00.000', 'SCH_ACTIVITY', NULL, NULL, NULL, NULL, NULL, NULL, 'PENDING', NULL, NULL);

INSERT INTO scheduled_instance
(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, comment, time_slot)
VALUES(43, 1, '2024-03-07', '08:30:00', '11:45:00', false, NULL, NULL, NULL, NULL, 'AM');
