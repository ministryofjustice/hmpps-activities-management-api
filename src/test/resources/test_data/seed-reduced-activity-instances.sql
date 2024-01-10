insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, created_time, created_by, paid)
values (1, 'PVI', 1, 1, true, false, false, false, 'H', 'Maths', 'Maths Level 1', '2022-10-10', null, 'high', '2022-9-21 00:00:00', 'SEED USER', true);

insert into activity_pay(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
values (1, 1, 'BAS', 'Basic', 11, 125, 150, 1);

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity, start_date)
values (1, 1, 'Maths AM', 1, 'L1', 'Location 1', 10, '2022-10-10');

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag, tuesday_flag, wednesday_flag, thursday_flag, friday_flag, saturday_flag, sunday_flag)
values (1, 1, '09:00:00', '12:00:00', true, true, true, true, true, true, true);

insert into scheduled_instance(activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, comment)
values (1, current_date, '09:00:00', '12:00:00', false, null, null, null, null);

insert into scheduled_instance(activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, comment)
values (1, current_date - 1, '09:00:00', '12:00:00', false, null, null, null, null);

insert into scheduled_instance(activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, comment)
values (1, current_date - 2, '09:00:00', '12:00:00', false, null, null, null, null);

insert into scheduled_instance(activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, comment)
values (1, current_date - 3, '09:00:00', '12:00:00', false, null, null, null, null);

insert into scheduled_instance(activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, comment)
values (1, current_date - 4, '09:00:00', '12:00:00', false, null, null, null, null);

insert into scheduled_instance(activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, comment)
values (1, current_date - 5, '09:00:00', '12:00:00', false, null, null, null, null);

insert into scheduled_instance(activity_schedule_id, session_date, start_time, end_time, cancelled, cancelled_time, cancelled_by, cancelled_reason, comment)
values (1, current_date - 6, '09:00:00', '12:00:00', false, null, null, null, null);
