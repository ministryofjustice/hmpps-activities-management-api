insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, created_time, created_by, paid)
values (1, 'PVI', 1, 1, true, false, false, false, 'H', 'Maths', 'Maths Level 1', '2022-10-10', null, 'high', '2022-9-21 00:00:00', 'SEED USER', false);

insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, created_time, created_by, paid)
values (2, 'PVI', 2, 2, true, false, false, false, 'H', 'Maths', 'English Level 1', '2022-10-10', null, 'high', '2022-9-21 00:00:00', 'SEED USER', false);

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (1, 1, 'Maths AM', 1, 'L1', 'Location 1', 10, '2022-10-10');

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag, time_slot)
values (1, 1, '10:00:00', '11:00:00', true, 'AM');

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (2, 1, 'Maths PM', 2, 'L2', 'Location 2', 10, '2022-10-10');

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag, time_slot)
values (2, 2, '10:00:00', '11:00:00', true, 'AM');

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (3, 2, 'English AM', 1, 'L1', 'Location 1', 10, '2022-10-10');

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag, time_slot)
values (3, 3, '14:00:00', '15:00:00', true, 'PM');

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, comment, time_slot)
values (1, 1, now()::date, '10:00:00', '11:00:00', true, now(), 'USER1', 'Location unavailable', null, 'AM');

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, comment, time_slot)
values (2, 2, '2022-10-10', '14:00:00', '15:00:00', true, now(), 'USER1', 'Location unavailable', null, 'PM');

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, comment, time_slot)
values (3, 2, now()::date, '14:00:00', '15:00:00', false, null, null, null, null, 'PM');

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, comment, time_slot)
values (4, 3, current_timestamp + interval '2 day', '14:00:00', '15:00:00', true, now(), 'USER1', 'Location unavailable', null, 'AM');

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, comment, time_slot)
values (5, 2, current_timestamp + interval '3 day', '17:00:00', '18:00:00', false, null, null, null, null, 'ED');

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, comment, time_slot)
values (6, 1, current_timestamp + interval '3 day', '17:00:00', '18:00:00', true, now(), 'USER1', 'Location unavailable', null, 'ED');

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, comment, time_slot)
values (7, 1, current_timestamp + interval '1 day', '13:00:00', '14:00:00', true, now(), 'USER1', 'Location unavailable', null, 'PM');

insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces, incentive_level_warning_issued, case_note_id, other_absence_reason, issue_payment)
values (1, 1, 'A11111A', null, 'comm1', now(), 'USER1', 'COMPLETED', null, 33, 1, true, 1, 'oar1', true);

insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces, incentive_level_warning_issued, case_note_id, other_absence_reason, issue_payment)
values (2, 1, 'A22222A', null, 'comm2', now(), 'USER1', 'COMPLETED', null, 44, 2, true, 2, 'oar2', false);

insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces, incentive_level_warning_issued, case_note_id, other_absence_reason, issue_payment)
values (3, 4, 'Y33333Y', null, 'comm3', now(), 'USER1', 'COMPLETED', null, 55, 4, true, 3, 'oar3', true);

insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces, incentive_level_warning_issued, case_note_id, other_absence_reason, issue_payment)
values (4, 5, 'Z44444Z', null, 'comm4', null, null, 'WAITING', null, 66, 5, true, 4, 'oar4', false);

insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces, incentive_level_warning_issued, case_note_id, other_absence_reason, issue_payment)
values (5, 6, 'Z44444Z', null, 'comm4', null, null, 'WAITING', null, 66, 5, true, 4, 'oar4', false);

insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces, incentive_level_warning_issued, case_note_id, other_absence_reason, issue_payment)
values (6, 7, 'Z5555Z', 7, 'comm4', null, null, 'WAITING', null, 66, 5, true, 4, 'oar4', false);

insert into attendance(attendance_id, scheduled_instance_id, prisoner_number, attendance_reason_id, comment, recorded_time, recorded_by, status, pay_amount, bonus_amount, pieces, incentive_level_warning_issued, case_note_id, other_absence_reason, issue_payment)
values (7, 7, 'Z66666Z', 10, 'comm4', null, null, 'WAITING', null, 66, 5, true, 4, 'oar4', false);
