insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, created_time, created_by, paid)
values (1, 'PVI', 1, 1, true, false, false, false, 'H', 'Maths', 'Maths Level 1', '2022-10-10', null, 'high', '2022-9-21 00:00:00', 'SEED USER', true);

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (1, 1, 'Maths AM', 1, 'L1', 'Location 1', 10, '2022-10-10');

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag, time_slot)
values (1, 1, '10:00:00', '11:00:00', true, 'AM');

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, cancelled_issue_payment, comment, time_slot)
values (1, 1, now()::date, '10:00:00', '11:00:00', true, current_timestamp - interval '1 day', 'Old user', 'Old reason', false, null, 'AM');

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, cancelled_issue_payment, comment, time_slot)
values (2, 1, now()::date, '13:00:00', '15:00:00', true, current_timestamp - interval '1 day', 'Old user', 'Old reason', false, null, 'PM');

insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces, incentive_level_warning_issued, case_note_id, other_absence_reason, issue_payment)
values (1, 1, 'A11111A', 8, 'Old comment', current_timestamp - interval '1 day', 'Old user', 'COMPLETED', null, 33, 1, true, 1, 'oar1', false);

insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces, incentive_level_warning_issued, case_note_id, other_absence_reason, issue_payment)
values (2, 2, 'Z22222A', 1, 'Old comment', current_timestamp - interval '1 day', 'Old user', 'COMPLETED', null, 33, 1, true, 1, 'oar1', false);

insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces, incentive_level_warning_issued, case_note_id, other_absence_reason, issue_payment)
values (3, 2, 'Z22222B', 2, 'Old comment', current_timestamp - interval '1 day', 'Old user', 'COMPLETED', null, 33, 1, true, 1, 'oar1', false);

insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces, incentive_level_warning_issued, case_note_id, other_absence_reason, issue_payment)
values (4, 2, 'Z22222C', 3, 'Old comment', current_timestamp - interval '1 day', 'Old user', 'COMPLETED', null, 33, 1, true, 1, 'oar1', false);

insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces, incentive_level_warning_issued, case_note_id, other_absence_reason, issue_payment)
values (5, 2, 'Z22222D', 4, 'Old comment', current_timestamp - interval '1 day', 'Old user', 'COMPLETED', null, 33, 1, true, 1, 'oar1', false);

insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces, incentive_level_warning_issued, case_note_id, other_absence_reason, issue_payment)
values (6, 2, 'Z22222E', 5, 'Old comment', current_timestamp - interval '1 day', 'Old user', 'COMPLETED', null, 33, 1, true, 1, 'oar1', false);

insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces, incentive_level_warning_issued, case_note_id, other_absence_reason, issue_payment)
values (7, 2, 'Z22222F', 6, 'Old comment', current_timestamp - interval '1 day', 'Old user', 'COMPLETED', null, 33, 1, true, 1, 'oar1', false);

insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces, incentive_level_warning_issued, case_note_id, other_absence_reason, issue_payment)
values (8, 2, 'Z22222G', 7, 'Old comment', current_timestamp - interval '1 day', 'Old user', 'COMPLETED', null, 33, 1, true, 1, 'oar1', false);

insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces, incentive_level_warning_issued, case_note_id, other_absence_reason, issue_payment)
values (9, 2, 'Z22222H', 8, 'Old comment', current_timestamp - interval '1 day', 'Old user', 'COMPLETED', null, 33, 1, true, 1, 'oar1', false);

insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces, incentive_level_warning_issued, case_note_id, other_absence_reason, issue_payment)
values (10, 2, 'Z22222I', 9, 'Old comment', current_timestamp - interval '1 day', 'Old user', 'COMPLETED', null, 33, 1, true, 1, 'oar1', false);

insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces, incentive_level_warning_issued, case_note_id, other_absence_reason, issue_payment)
values (11, 2, 'Z22222J', 10, 'Old comment', current_timestamp - interval '1 day', 'Old user', 'COMPLETED', null, 33, 1, true, 1, 'oar1', false);


