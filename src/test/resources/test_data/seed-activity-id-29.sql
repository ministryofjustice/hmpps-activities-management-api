INSERT INTO activity
(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, created_time, created_by, updated_time, updated_by, on_wing, off_wing, activity_organiser_id, paid)
VALUES(558, 'WDI', 9, 1, true, false, false, false, 'H', 'Library Access sessions', 'Library Access sessions', '2024-05-20', NULL, 'high', '2024-05-14 11:30:52.412', 'TQJ73Y', '2024-05-16 11:38:41.655', 'TQJ73Y', false, false, NULL, true);

INSERT INTO activity_schedule
(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date, end_date, runs_on_bank_holiday, updated_time, updated_by, instances_last_updated_time, schedule_weeks)
VALUES(558, 558, 'Library Access sessions', 15536, 'LIBRARY', 'LIBRARY', 80, '2024-05-20', NULL, true, '2024-05-16 11:38:41.655', 'TQJ73Y', '2024-06-21 04:00:58.157', 1);

INSERT INTO allocation
(allocation_id, activity_schedule_id, prisoner_number, booking_id, prison_pay_band_id, start_date, end_date, allocated_time, allocated_by, deallocated_time, deallocated_by, deallocated_reason, suspended_time, suspended_by, suspended_reason, prisoner_status, planned_deallocation_id, deallocation_case_note_id)
VALUES(17657, 558, 'A4786AJ', 80947, 1, '2024-05-23', NULL, '2024-05-16 13:04:00.000', 'TQJ73Y', NULL, NULL, NULL, NULL, NULL, NULL, 'ACTIVE', NULL, NULL);

INSERT INTO scheduled_instance
(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, "comment", time_slot)
VALUES(122405, 558, now(), '13:45:00', '16:30:00', true, current_timestamp - interval '5 days', 'MQD05I', 'Staff unavailable', '', 'PM');

INSERT INTO attendance
(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, "comment", recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces, issue_payment, case_note_id, incentive_level_warning_issued, other_absence_reason)
VALUES(680879, 122405, 'A4786AJ', 8, 'Staff unavailable', current_timestamp - interval '5 days', 'MQD05I', 'COMPLETED', 159, NULL, NULL, true, NULL, NULL, NULL);

