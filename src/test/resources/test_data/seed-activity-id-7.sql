insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, summary, description, start_date, end_date, risk_level, minimum_incentive_level, created_time, created_by)
values (1, 'MDI', 1, 1, true, false, false, false, 'Maths', 'Maths Level 1', '2022-10-10', null, null, null, '2022-9-21 00:00:00', 'SEED USER');

insert into activity_pay(activity_pay_id, activity_id, incentive_level, pay_band, rate, piece_rate, piece_rate_items)
values (1, 1, 'BAS', 'A', 125, 150, 1);

insert into activity_schedule(activity_schedule_id, activity_id, description, internal_location_id, internal_location_code, internal_location_description, capacity)
values (1, 1, 'Maths AM', 1, 'L1', 'Location 1', 10);

insert into activity_schedule_slot(activity_schedule_slot_id, activity_schedule_id, start_time, end_time, monday_flag)
values (1, 1, '10:00:00', '11:00:00', true);