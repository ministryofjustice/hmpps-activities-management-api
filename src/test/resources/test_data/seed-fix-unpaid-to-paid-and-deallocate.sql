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

INSERT INTO public.allocation
(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status, planned_deallocation_id, deallocation_case_note_id)
VALUES(920, 129, 'A8862DW', 1153507, 6, '2023-09-30', NULL, '2023-09-29 16:50:00.000', 'MIGRATION', NULL, NULL, NULL, NULL, NULL, NULL, 'ACTIVE', NULL, NULL);
INSERT INTO public.allocation
(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status, planned_deallocation_id, deallocation_case_note_id)
VALUES(1263, 129, 'A0334EZ', 2853825, 6, '2023-10-06', NULL, '2023-10-05 10:25:00.000', 'FQO13N', NULL, NULL, NULL, NULL, NULL, NULL, 'ACTIVE', NULL, NULL);
INSERT INTO public.allocation
(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status, planned_deallocation_id, deallocation_case_note_id)
VALUES(1119, 129, 'A1611AF', 46135, 6, '2023-09-30', NULL, '2023-09-29 16:50:00.000', 'MIGRATION', NULL, NULL, NULL, NULL, NULL, NULL, 'ACTIVE', NULL, NULL);
INSERT INTO public.allocation
(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status, planned_deallocation_id, deallocation_case_note_id)
VALUES(19874, 129, 'A6015FC', 2916081, 6, '2024-06-11', NULL, '2024-06-10 15:15:00.000', 'NQY96W', NULL, NULL, NULL, NULL, NULL, NULL, 'ACTIVE', NULL, NULL);
INSERT INTO public.allocation
(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status, planned_deallocation_id, deallocation_case_note_id)
VALUES(24410, 129, 'A7345CR', 2820627, 6, '2024-08-01', NULL, '2024-07-31 11:40:00.000', 'NQY96W', NULL, NULL, NULL, NULL, NULL, NULL, 'ACTIVE', NULL, NULL);
INSERT INTO public.allocation
(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status, planned_deallocation_id, deallocation_case_note_id)
VALUES(13529, 129, 'A4425FC', 2912105, 6, '2024-04-05', NULL, '2024-03-28 08:45:00.000', 'VQI30O', NULL, NULL, NULL, NULL, NULL, NULL, 'ACTIVE', NULL, NULL);
INSERT INTO public.allocation
(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status, planned_deallocation_id, deallocation_case_note_id)
VALUES(19365, 129, 'A1590FA', 2881002, 6, '2024-06-13', NULL, '2024-06-05 08:03:00.000', 'NQY96W', NULL, NULL, NULL, NULL, NULL, NULL, 'ACTIVE', NULL, NULL);
INSERT INTO public.allocation
(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status, planned_deallocation_id, deallocation_case_note_id)
VALUES(19692, 129, 'A5091ER', 2913798, 6, '2024-06-18', NULL, '2024-06-10 08:04:00.000', 'NQY96W', NULL, NULL, NULL, NULL, NULL, NULL, 'ACTIVE', NULL, NULL);
INSERT INTO public.allocation
(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status, planned_deallocation_id, deallocation_case_note_id)
VALUES(21159, 129, 'A5022DQ', 2808713, 4, '2024-07-03', NULL, '2024-06-25 09:14:00.000', 'RQZ82W', NULL, NULL, NULL, NULL, NULL, NULL, 'ACTIVE', NULL, NULL);
INSERT INTO public.allocation
(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status, planned_deallocation_id, deallocation_case_note_id)
VALUES(1121, 129, 'A4812DT', 1102885, 6, '2023-09-30', NULL, '2023-09-29 16:50:00.000', 'MIGRATION', NULL, NULL, NULL, NULL, NULL, NULL, 'ACTIVE', NULL, NULL);
INSERT INTO public.allocation
(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status, planned_deallocation_id, deallocation_case_note_id)
VALUES(758, 129, 'A1798EF', 2388295, 6, '2023-09-30', NULL, '2023-09-29 16:50:00.000', 'MIGRATION', NULL, NULL, NULL, NULL, NULL, NULL, 'ACTIVE', NULL, NULL);
INSERT INTO public.allocation
(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status, planned_deallocation_id, deallocation_case_note_id)
VALUES(21158, 129, 'A2221CW', 718204, 4, '2024-07-03', NULL, '2024-06-25 09:14:00.000', 'RQZ82W', NULL, NULL, NULL, NULL, NULL, NULL, 'ACTIVE', NULL, NULL);
INSERT INTO public.allocation
(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status, planned_deallocation_id, deallocation_case_note_id)
VALUES(1048, 129, 'A8764EV', 2780214, 6, '2023-09-30', NULL, '2023-09-29 16:50:00.000', 'MIGRATION', NULL, NULL, NULL, NULL, NULL, NULL, 'ACTIVE', NULL, NULL);
INSERT INTO public.allocation
(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status, planned_deallocation_id, deallocation_case_note_id)
VALUES(854, 129, 'A3840EA', 1227745, 6, '2023-09-30', NULL, '2023-09-29 16:50:00.000', 'MIGRATION', NULL, NULL, NULL, NULL, NULL, NULL, 'ACTIVE', NULL, NULL);
INSERT INTO public.allocation
(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status, planned_deallocation_id, deallocation_case_note_id)
VALUES(23274, 129, 'A6655AQ', 2935791, 6, '2024-07-25', NULL, '2024-07-17 08:00:00.000', 'NQY96W', NULL, NULL, NULL, NULL, NULL, NULL, 'ACTIVE', NULL, NULL);
INSERT INTO public.allocation
(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status, planned_deallocation_id, deallocation_case_note_id)
VALUES(15304, 129, 'A3316CV', 2887278, 6, '2024-04-30', NULL, '2024-04-22 07:46:00.000', 'NQY96W', NULL, NULL, NULL, NULL, NULL, NULL, 'ACTIVE', NULL, NULL);
INSERT INTO public.allocation
(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status, planned_deallocation_id, deallocation_case_note_id)
VALUES(18707, 129, 'A0593FD', 2927363, 6, '2024-06-06', NULL, '2024-05-29 07:59:00.000', 'NQY96W', NULL, NULL, NULL, NULL, NULL, NULL, 'ACTIVE', NULL, NULL);
INSERT INTO public.allocation
(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status, planned_deallocation_id, deallocation_case_note_id)
VALUES(24025, 129, 'A4249DC', 2886387, 4, '2024-08-03', NULL, '2024-07-26 08:06:00.000', 'RQZ82W', NULL, NULL, NULL, NULL, NULL, NULL, 'ACTIVE', NULL, NULL);
INSERT INTO public.allocation
(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status, planned_deallocation_id, deallocation_case_note_id)
VALUES(24663, 129, 'A7188AE', 41646, 6, '2024-08-03', NULL, '2024-08-02 11:39:00.000', 'NQY96W', NULL, NULL, NULL, NULL, NULL, NULL, 'ACTIVE', NULL, NULL);
INSERT INTO public.allocation
(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status, planned_deallocation_id, deallocation_case_note_id)
VALUES(24219, 129, 'A3161FD', 2933855, 4, '2024-08-07', NULL, '2024-07-30 08:49:00.000', 'RQZ82W', NULL, NULL, NULL, NULL, NULL, NULL, 'ACTIVE', NULL, NULL);
INSERT INTO public.allocation
(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status, planned_deallocation_id, deallocation_case_note_id)
VALUES(24593, 129, 'A4774FD', 2937945, 6, now()::date + 3, NULL, '2024-08-02 08:08:00.000', 'NQY96W', NULL, NULL, NULL, NULL, NULL, NULL, 'PENDING', NULL, NULL);
INSERT INTO public.allocation
(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status, planned_deallocation_id, deallocation_case_note_id)
VALUES(12129, 129, 'A2902EY', 2836175, 6, '2024-03-20', NULL, '2024-03-12 08:52:00.000', 'VQI30O', NULL, NULL, NULL, '2024-09-06 16:49:38.017', 'Activities Management Service', 'Temporarily released or transferred', 'AUTO_SUSPENDED', NULL, NULL);
INSERT INTO public.allocation
(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status, planned_deallocation_id, deallocation_case_note_id)
VALUES(28871, 129, 'A3322FA', 2885100, 4, now()::date + 3, now()::date + 12, '2024-09-12 09:59:00.000', 'RQZ82W', '2024-09-20 02:00:19.000', 'RQZ82W', 'PLANNED', NULL, NULL, NULL, 'ENDED', NULL, NULL);
