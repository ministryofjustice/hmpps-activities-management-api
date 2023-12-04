--
-- One activity with one schedule, one slot and one instance but no allocations
--
insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, minimum_incentive_nomis_code, minimum_incentive_level, created_time, created_by, paid)
values (10, 'MDI', 2, 2, true, false, false, false, 'H', 'Geography', 'Geography Level 1', '2022-10-01', null, 'high', 'BAS', 'Basic', '2022-9-21 00:00:00', 'SEED USER', true);

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (1, 10, 'Geography AM', 1, 'L1', 'Location MDI 1', 10, '2022-10-01');

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag)
values (1, 1, '09:00:00', '12:00:00', true);

insert into scheduled_instance(scheduled_instance_id, activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, comment)
values (1, 1, '2022-10-01', '09:00:00', '12:00:00', false, null, null, null, null);
