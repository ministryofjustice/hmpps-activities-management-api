INSERT INTO public.activity
(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, created_time, created_by, updated_time, updated_by, on_wing, off_wing, activity_organiser_id, paid)
VALUES(135, 'RSI', 8, 3, true, true, false, false, 'H', 'Long Term Sick', 'Long Term Sick', '2023-09-30', NULL, 'low', '2023-09-29 15:57:07.661', 'MIGRATION', '2023-11-14 10:53:16.214', 'activities-management-admin-1', false, false, NULL, true);


INSERT INTO public.activity_schedule
(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date, end_date, runs_on_bank_holiday, updated_time, updated_by, instances_last_updated_time, schedule_weeks)
VALUES(151, 135, 'Long Term Sick', 789671, 'STAYONWING', 'RSI-OTHER-STAYONWING', 10, '2023-09-30', NULL, true, '2023-11-14 10:53:16.214', 'activities-management-admin-1', '2024-07-15 04:00:19.257', 1);

INSERT INTO public.activity_schedule_slot
(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag, tuesday_flag, wednesday_flag, thursday_flag, friday_flag, saturday_flag, sunday_flag, week_number)
VALUES(225, 151, '08:30:00', '11:45:00', true, true, true, true, true, false, false, 1);
INSERT INTO public.activity_schedule_slot
(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag, tuesday_flag, wednesday_flag, thursday_flag, friday_flag, saturday_flag, sunday_flag, week_number)
VALUES(235, 151, '13:45:00', '16:45:00', true, true, true, true, true, false, false, 1);

INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
VALUES(412, 135, 'BAS', 'Basic', 4, 0, NULL, NULL);
INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
VALUES(416, 135, 'STD', 'Standard', 4, 0, NULL, NULL);
INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
VALUES(424, 135, 'ENH', 'Enhanced', 4, 0, NULL, NULL);
INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
VALUES(441, 135, 'BAS', 'Basic', 5, 0, NULL, NULL);
INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
VALUES(446, 135, 'STD', 'Standard', 5, 0, NULL, NULL);
INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
VALUES(447, 135, 'ENH', 'Enhanced', 5, 0, NULL, NULL);
INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
VALUES(448, 135, 'BAS', 'Basic', 6, 0, NULL, NULL);
INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
VALUES(450, 135, 'STD', 'Standard', 6, 0, NULL, NULL);
INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
VALUES(452, 135, 'ENH', 'Enhanced', 6, 0, NULL, NULL);
INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
VALUES(454, 135, 'BAS', 'Basic', 7, 0, NULL, NULL);
INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
VALUES(457, 135, 'STD', 'Standard', 7, 0, NULL, NULL);
INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
VALUES(461, 135, 'ENH', 'Enhanced', 7, 0, NULL, NULL);
INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
VALUES(464, 135, 'BAS', 'Basic', 8, 0, NULL, NULL);
INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
VALUES(467, 135, 'STD', 'Standard', 8, 0, NULL, NULL);
INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
VALUES(481, 135, 'ENH', 'Enhanced', 8, 0, NULL, NULL);
INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
VALUES(482, 135, 'BAS', 'Basic', 9, 0, NULL, NULL);
INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
VALUES(483, 135, 'STD', 'Standard', 9, 0, NULL, NULL);
INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
VALUES(484, 135, 'ENH', 'Enhanced', 9, 0, NULL, NULL);
INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
VALUES(485, 135, 'BAS', 'Basic', 10, 0, NULL, NULL);
INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
VALUES(486, 135, 'STD', 'Standard', 10, 0, NULL, NULL);
INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
VALUES(487, 135, 'ENH', 'Enhanced', 10, 0, NULL, NULL);
INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
VALUES(488, 135, 'BAS', 'Basic', 11, 0, NULL, NULL);
INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
VALUES(489, 135, 'STD', 'Standard', 11, 0, NULL, NULL);
INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
VALUES(491, 135, 'ENH', 'Enhanced', 11, 0, NULL, NULL);
INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
VALUES(492, 135, 'BAS', 'Basic', 12, 0, NULL, NULL);
INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
VALUES(493, 135, 'STD', 'Standard', 12, 0, NULL, NULL);
INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
VALUES(494, 135, 'ENH', 'Enhanced', 12, 0, NULL, NULL);
INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
VALUES(498, 135, 'BAS', 'Basic', 13, 0, NULL, NULL);
INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
VALUES(502, 135, 'STD', 'Standard', 13, 0, NULL, NULL);
INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
VALUES(504, 135, 'ENH', 'Enhanced', 13, 0, NULL, NULL);

INSERT INTO public.allocation
(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status, planned_deallocation_id, deallocation_case_note_id)
VALUES(1124, 151, 'A7175CH', 541729, 6, '2023-09-30', NULL, '2023-09-29 16:50:00.000', 'MIGRATION', NULL, NULL, NULL, NULL, NULL, NULL, 'ACTIVE', NULL, NULL);
INSERT INTO public.allocation
(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status, planned_deallocation_id, deallocation_case_note_id)
VALUES(2208, 151, 'A3903DM', 2747785, 6, '2023-11-15', NULL, '2023-11-14 10:52:00.000', 'FQO13N', NULL, NULL, NULL, NULL, NULL, NULL, 'ACTIVE', NULL, NULL);
INSERT INTO public.allocation
(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status, planned_deallocation_id, deallocation_case_note_id)
VALUES(20980, 151, 'A2539EW', 2788935, 6, '2024-06-22', NULL, '2024-06-21 10:31:00.000', 'FQO13N', NULL, NULL, NULL, NULL, NULL, NULL, 'ACTIVE', NULL, NULL);
INSERT INTO public.allocation
(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status, planned_deallocation_id, deallocation_case_note_id)
VALUES(12670, 151, 'A3084EX', 2813524, 6, '2024-03-20', NULL, '2024-03-19 07:51:00.000', 'FQO13N', NULL, NULL, NULL, NULL, NULL, NULL, 'ACTIVE', NULL, NULL);
INSERT INTO public.allocation
(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status, planned_deallocation_id, deallocation_case_note_id)
VALUES(18501, 151, 'A4778DA', 2840637, 6, '2024-05-25', NULL, '2024-05-24 15:03:00.000', 'FQO13N', NULL, NULL, NULL, NULL, NULL, NULL, 'ACTIVE', NULL, NULL);
INSERT INTO public.allocation
(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status, planned_deallocation_id, deallocation_case_note_id)
VALUES(23073, 151, 'A5617CQ', 2889202, 6, '2024-07-16', NULL, '2024-07-15 08:45:00.000', 'FQO13N', NULL, NULL, NULL, NULL, NULL, NULL, 'ACTIVE', NULL, NULL);

-- INSERT INTO public.planned_deallocation
-- (planned_deallocation_id, planned_date, planned_by, planned_reason, planned_at, allocation_id, case_note_id)
-- VALUES(14, '2024-07-29', 'TEST_USER', 'OTHER', '2024-07-29 16:24:30.710', 18501, NULL);
-- INSERT INTO public.planned_deallocation
-- (planned_deallocation_id, planned_date, planned_by, planned_reason, planned_at, allocation_id, case_note_id)
-- VALUES(15, '2024-07-29', 'TEST_USER', 'OTHER', '2024-07-29 16:24:30.710', 12670, NULL);
-- INSERT INTO public.planned_deallocation
-- (planned_deallocation_id, planned_date, planned_by, planned_reason, planned_at, allocation_id, case_note_id)
-- VALUES(16, '2024-07-29', 'TEST_USER', 'OTHER', '2024-07-29 16:24:30.710', 20980, NULL);
-- INSERT INTO public.planned_deallocation
-- (planned_deallocation_id, planned_date, planned_by, planned_reason, planned_at, allocation_id, case_note_id)
-- VALUES(17, '2024-07-29', 'TEST_USER', 'OTHER', '2024-07-29 16:24:30.710', 2208, NULL);
-- INSERT INTO public.planned_deallocation
-- (planned_deallocation_id, planned_date, planned_by, planned_reason, planned_at, allocation_id, case_note_id)
-- VALUES(18, '2024-07-29', 'TEST_USER', 'OTHER', '2024-07-29 16:24:30.710', 1124, NULL);
-- INSERT INTO public.planned_deallocation
-- (planned_deallocation_id, planned_date, planned_by, planned_reason, planned_at, allocation_id, case_note_id)
-- VALUES(19, '2024-07-29', 'TEST_USER', 'OTHER', '2024-07-29 16:24:30.709', 23073, NULL);

-- update allocation set planned_deallocation_id = 19 where allocation_id = 23073;
-- update allocation set planned_deallocation_id = 18 where allocation_id = 1124;
-- update allocation set planned_deallocation_id = 17 where allocation_id = 2208;
-- update allocation set planned_deallocation_id = 16 where allocation_id = 20980;
-- update allocation set planned_deallocation_id = 15 where allocation_id = 12670;
-- update allocation set planned_deallocation_id = 14 where allocation_id = 18501;


INSERT INTO public.scheduled_instance
(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, "comment")
VALUES(124003, 151, '2024-07-15', '08:30:00', '11:45:00', false, NULL, NULL, NULL, NULL);
INSERT INTO public.scheduled_instance
(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, "comment")
VALUES(124004, 151, '2024-07-15', '13:45:00', '16:45:00', false, NULL, NULL, NULL, NULL);
INSERT INTO public.scheduled_instance
(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, "comment")
VALUES(124815, 151, '2024-07-16', '08:30:00', '11:45:00', false, NULL, NULL, NULL, NULL);
INSERT INTO public.scheduled_instance
(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, "comment")
VALUES(124816, 151, '2024-07-16', '13:45:00', '16:45:00', false, NULL, NULL, NULL, NULL);
INSERT INTO public.scheduled_instance
(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, "comment")
VALUES(125510, 151, '2024-07-17', '08:30:00', '11:45:00', false, NULL, NULL, NULL, NULL);
INSERT INTO public.scheduled_instance
(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, "comment")
VALUES(125511, 151, '2024-07-17', '13:45:00', '16:45:00', false, NULL, NULL, NULL, NULL);
INSERT INTO public.scheduled_instance
(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, "comment")
VALUES(126222, 151, '2024-07-18', '08:30:00', '11:45:00', false, NULL, NULL, NULL, NULL);
INSERT INTO public.scheduled_instance
(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, "comment")
VALUES(126223, 151, '2024-07-18', '13:45:00', '16:45:00', false, NULL, NULL, NULL, NULL);
INSERT INTO public.scheduled_instance
(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, "comment")
VALUES(126861, 151, '2024-07-19', '08:30:00', '11:45:00', false, NULL, NULL, NULL, NULL);
INSERT INTO public.scheduled_instance
(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, "comment")
VALUES(126862, 151, '2024-07-19', '13:45:00', '16:45:00', false, NULL, NULL, NULL, NULL);
INSERT INTO public.scheduled_instance
(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, "comment")
VALUES(127887, 151, '2024-07-22', '08:30:00', '11:45:00', false, NULL, NULL, NULL, NULL);
INSERT INTO public.scheduled_instance
(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, "comment")
VALUES(127888, 151, '2024-07-22', '13:45:00', '16:45:00', false, NULL, NULL, NULL, NULL);
INSERT INTO public.scheduled_instance
(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, "comment")
VALUES(128631, 151, '2024-07-23', '08:30:00', '11:45:00', false, NULL, NULL, NULL, NULL);
INSERT INTO public.scheduled_instance
(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, "comment")
VALUES(128632, 151, '2024-07-23', '13:45:00', '16:45:00', false, NULL, NULL, NULL, NULL);
INSERT INTO public.scheduled_instance
(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, "comment")
VALUES(129337, 151, '2024-07-24', '08:30:00', '11:45:00', false, NULL, NULL, NULL, NULL);
INSERT INTO public.scheduled_instance
(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, "comment")
VALUES(129338, 151, '2024-07-24', '13:45:00', '16:45:00', false, NULL, NULL, NULL, NULL);
INSERT INTO public.scheduled_instance
(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, "comment")
VALUES(130056, 151, '2024-07-25', '08:30:00', '11:45:00', false, NULL, NULL, NULL, NULL);
INSERT INTO public.scheduled_instance
(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, "comment")
VALUES(130057, 151, '2024-07-25', '13:45:00', '16:45:00', false, NULL, NULL, NULL, NULL);
INSERT INTO public.scheduled_instance
(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, "comment")
VALUES(130684, 151, '2024-07-26', '08:30:00', '11:45:00', false, NULL, NULL, NULL, NULL);
INSERT INTO public.scheduled_instance
(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, "comment")
VALUES(130685, 151, '2024-07-26', '13:45:00', '16:45:00', false, NULL, NULL, NULL, NULL);
INSERT INTO public.scheduled_instance
(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, "comment")
VALUES(131725, 151, '2024-07-29', '08:30:00', '11:45:00', false, NULL, NULL, NULL, NULL);
INSERT INTO public.scheduled_instance
(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, "comment")
VALUES(131726, 151, '2024-07-29', '13:45:00', '16:45:00', false, NULL, NULL, NULL, NULL);


INSERT INTO public.attendance
(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, "comment", recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces, issue_payment, case_note_id, incentive_level_warning_issued, other_absence_reason)
VALUES(759859, 124003, 'A3903DM', NULL, NULL, NULL, NULL, 'WAITING', 0, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO public.attendance
(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, "comment", recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces, issue_payment, case_note_id, incentive_level_warning_issued, other_absence_reason)
VALUES(759862, 124003, 'A4778DA', NULL, NULL, NULL, NULL, 'WAITING', 0, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO public.attendance
(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, "comment", recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces, issue_payment, case_note_id, incentive_level_warning_issued, other_absence_reason)
VALUES(759858, 124003, 'A7175CH', 9, NULL, '2024-07-15 07:00:03.104', 'IQV07E', 'COMPLETED', 0, NULL, NULL, true, NULL, NULL, NULL);
INSERT INTO public.attendance
(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, "comment", recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces, issue_payment, case_note_id, incentive_level_warning_issued, other_absence_reason)
VALUES(759860, 124003, 'A2539EW', 9, NULL, '2024-07-15 07:00:03.106', 'IQV07E', 'COMPLETED', 0, NULL, NULL, true, NULL, NULL, NULL);
INSERT INTO public.attendance
(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, "comment", recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces, issue_payment, case_note_id, incentive_level_warning_issued, other_absence_reason)
VALUES(759861, 124003, 'A3084EX', 9, NULL, '2024-07-15 07:00:03.108', 'IQV07E', 'COMPLETED', 0, NULL, NULL, true, NULL, NULL, NULL);