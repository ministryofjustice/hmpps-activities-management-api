insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, minimum_incentive_nomis_code, minimum_incentive_level, created_time, created_by)
values (9, 'MDI', 1, 1, true, true, true, true, 'H', 'Maths', 'Maths Level 1', current_date, null, 'high', 'BAS', 'Basic', current_timestamp, 'SEED USER');

insert into activity_pay(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
values (1, 9, 'BAS', 'Basic', 1, 125, 150, 1);
