INSERT INTO public.activity
(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, created_time, created_by, updated_time, updated_by, on_wing, off_wing, activity_organiser_id, paid)
VALUES(119, 'RSI', 8, 3, true, true, false, false, 'H', 'Retired Prisoners', 'Retired Prisoners', '2023-09-30', NULL, 'low', '2023-09-29 15:57:07.613', 'MIGRATION', '2023-10-04 09:22:14.882', 'activities-management-admin-1', false, false, NULL, true);

INSERT INTO public.activity_schedule
(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date, end_date, runs_on_bank_holiday, updated_time, updated_by, instances_last_updated_time, schedule_weeks, use_prison_regime_time)
VALUES(129, 119, 'Retired Prisoners', 789670, 'RETIRED', 'RSI-OTHER-RETIRED', 20, '2023-09-30', NULL, true, '2023-10-04 09:22:14.882', 'activities-management-admin-1', '2024-08-12 04:00:16.711', 1, true);

INSERT INTO public.activity_schedule_slot
(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag, tuesday_flag, wednesday_flag, thursday_flag, friday_flag, saturday_flag, sunday_flag, week_number, time_slot)
VALUES(192, 129, '08:30:00', '11:45:00', true, true, true, true, true, false, false, 1, 'AM');
INSERT INTO public.activity_schedule_slot
(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag, tuesday_flag, wednesday_flag, thursday_flag, friday_flag, saturday_flag, sunday_flag, week_number, time_slot)
VALUES(193, 129, '13:45:00', '16:45:00', true, true, true, true, true, false, false, 1, 'PM');

INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items, start_date)
VALUES(334, 119, 'BAS', 'Basic', 4, 0, NULL, NULL, NULL);
INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items, start_date)
VALUES(337, 119, 'STD', 'Standard', 4, 0, NULL, NULL, NULL);
INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items, start_date)
VALUES(344, 119, 'ENH', 'Enhanced', 4, 0, NULL, NULL, NULL);
INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items, start_date)
VALUES(353, 119, 'BAS', 'Basic', 5, 0, NULL, NULL, NULL);
INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items, start_date)
VALUES(358, 119, 'STD', 'Standard', 5, 0, NULL, NULL, NULL);
INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items, start_date)
VALUES(361, 119, 'ENH', 'Enhanced', 5, 0, NULL, NULL, NULL);
INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items, start_date)
VALUES(367, 119, 'BAS', 'Basic', 6, 0, NULL, NULL, NULL);
INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items, start_date)
VALUES(372, 119, 'STD', 'Standard', 6, 0, NULL, NULL, NULL);
INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items, start_date)
VALUES(374, 119, 'ENH', 'Enhanced', 6, 0, NULL, NULL, NULL);
INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items, start_date)
VALUES(375, 119, 'BAS', 'Basic', 7, 0, NULL, NULL, NULL);
INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items, start_date)
VALUES(377, 119, 'STD', 'Standard', 7, 0, NULL, NULL, NULL);
INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items, start_date)
VALUES(378, 119, 'ENH', 'Enhanced', 7, 0, NULL, NULL, NULL);
INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items, start_date)
VALUES(381, 119, 'BAS', 'Basic', 8, 0, NULL, NULL, NULL);
INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items, start_date)
VALUES(384, 119, 'STD', 'Standard', 8, 0, NULL, NULL, NULL);
INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items, start_date)
VALUES(385, 119, 'ENH', 'Enhanced', 8, 0, NULL, NULL, NULL);
INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items, start_date)
VALUES(386, 119, 'BAS', 'Basic', 9, 0, NULL, NULL, NULL);
INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items, start_date)
VALUES(387, 119, 'STD', 'Standard', 9, 0, NULL, NULL, NULL);
INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items, start_date)
VALUES(388, 119, 'ENH', 'Enhanced', 9, 0, NULL, NULL, NULL);
INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items, start_date)
VALUES(389, 119, 'BAS', 'Basic', 10, 0, NULL, NULL, NULL);
INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items, start_date)
VALUES(390, 119, 'STD', 'Standard', 10, 0, NULL, NULL, NULL);
INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items, start_date)
VALUES(391, 119, 'ENH', 'Enhanced', 10, 0, NULL, NULL, NULL);
INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items, start_date)
VALUES(392, 119, 'BAS', 'Basic', 11, 0, NULL, NULL, NULL);
INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items, start_date)
VALUES(393, 119, 'STD', 'Standard', 11, 0, NULL, NULL, NULL);
INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items, start_date)
VALUES(394, 119, 'ENH', 'Enhanced', 11, 0, NULL, NULL, NULL);
INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items, start_date)
VALUES(395, 119, 'BAS', 'Basic', 12, 0, NULL, NULL, NULL);
INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items, start_date)
VALUES(396, 119, 'STD', 'Standard', 12, 0, NULL, NULL, NULL);
INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items, start_date)
VALUES(397, 119, 'ENH', 'Enhanced', 12, 0, NULL, NULL, NULL);
INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items, start_date)
VALUES(398, 119, 'BAS', 'Basic', 13, 0, NULL, NULL, NULL);
INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items, start_date)
VALUES(399, 119, 'STD', 'Standard', 13, 0, NULL, NULL, NULL);
INSERT INTO public.activity_pay
(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items, start_date)
VALUES(400, 119, 'ENH', 'Enhanced', 13, 0, NULL, NULL, NULL);

INSERT INTO public.data_fix (data_fix_id, activity_schedule_id, prisoner_number, start_date, prisoner_status) VALUES(1, 129, 'A8862DW', now()::date, 'ACTIVE');
INSERT INTO public.data_fix (data_fix_id, activity_schedule_id, prisoner_number, start_date, prisoner_status) VALUES(2, 129, 'A0334EZ', now()::date + 3, 'PENDING');
INSERT INTO public.data_fix (data_fix_id, activity_schedule_id, prisoner_number, start_date, prisoner_status) VALUES(3, 129, 'A1611AF', now()::date, 'AUTO_SUSPENDED');
INSERT INTO public.data_fix (data_fix_id, activity_schedule_id, prisoner_number, start_date, prisoner_status) VALUES(4, 129, 'A6015FC', now()::date, 'SUSPENDED');

INSERT INTO public.allocation
(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status, planned_deallocation_id, deallocation_case_note_id)
VALUES(920, 129, 'A8862DW', 1153507, 6, '2023-09-30', NULL, '2023-09-29 16:50:00.000', 'MIGRATION', '2024-07-15 08:45:00.000', 'TEST_USER', 'OTHER', NULL, NULL, NULL, 'ENDED', NULL, NULL);
INSERT INTO public.allocation
(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status, planned_deallocation_id, deallocation_case_note_id)
VALUES(1263, 129, 'A0334EZ', 2853825, 6, '2023-10-06', NULL, '2023-10-05 10:25:00.000', 'FQO13N', '2024-07-15 08:45:00.000', 'TEST_USER', 'OTHER', NULL, NULL, NULL, 'ENDED', NULL, NULL);
INSERT INTO public.allocation
(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status, planned_deallocation_id, deallocation_case_note_id)
VALUES(1119, 129, 'A1611AF', 46135, 6, '2023-09-30', NULL, '2023-09-29 16:50:00.000', 'MIGRATION', '2024-07-15 08:45:00.000', 'TEST_USER', 'OTHER', NULL, NULL, NULL, 'ENDED', NULL, NULL);
INSERT INTO public.allocation
(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status, planned_deallocation_id, deallocation_case_note_id)
VALUES(19874, 129, 'A6015FC', 2916081, 6, '2024-06-11', NULL, '2024-06-10 15:15:00.000', 'NQY96W', '2024-07-15 08:45:00.000', 'TEST_USER', 'OTHER', NULL, NULL, NULL, 'ENDED', NULL, NULL);

INSERT INTO public.scheduled_instance
(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, "comment", time_slot)
VALUES(140668, 129, now()::date + 1, '15:30:00', '17:45:00', false, NULL, NULL, NULL, NULL, 'PM');

INSERT INTO public.scheduled_instance
(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, "comment", time_slot)
VALUES(140669, 129, now()::date, '08:30:00', '11:45:00', false, NULL, NULL, NULL, NULL, 'AM');

INSERT INTO public.scheduled_instance
(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, "comment", time_slot)
VALUES(140670, 129, now()::date, '15:30:00', '17:45:00', false, NULL, NULL, NULL, NULL, 'PM');