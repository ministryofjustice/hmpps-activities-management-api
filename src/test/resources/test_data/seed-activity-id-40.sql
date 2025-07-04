insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, activity_organiser_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, created_time, created_by, paid)
values (1, 'PVI', 1, 2, 1, true, false, false, false, 'H', 'Maths', 'Maths Level 1', '2022-10-10', null, 'high', '2022-9-21 00:00:00', 'SEED USER', true);

insert into activity_pay(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
values (1, 1, 'BAS', 'Basic', 1, 125, 125, 1);

insert into activity_pay(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items, start_date)
values (2, 1, 'BAS', 'Basic', 1, 75, 75, 1, '2025-03-09');

insert into activity_pay(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items, start_date)
values (3, 1, 'BAS', 'Basic', 1, 200, 200, 1, '2025-05-07');

insert into activity_pay_history(activity_pay_history_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, start_date, changed_details, changed_time, changed_by)
values (1, 1, 'BAS', 'Basic', 1, 125, null, 'New pay rate added: £1.25', '2025-03-09 09:00:00', 'joebloggs');

insert into activity_pay_history(activity_pay_history_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, start_date, changed_details, changed_time, changed_by)
values (2, 1, 'BAS', 'Basic', 1, 75, '2025-03-09', 'Amount reduced to £0.75, from 9 Mar 2025', '2025-04-10 09:00:00', 'adsmith');

insert into activity_pay_history(activity_pay_history_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, start_date, changed_details, changed_time, changed_by)
values (3, 1, 'BAS', 'Basic', 1, 200, '2025-05-07', 'Amount increased to £2.00, from 7 May 2025', '2025-04-20 09:00:00', 'joebloggs');
