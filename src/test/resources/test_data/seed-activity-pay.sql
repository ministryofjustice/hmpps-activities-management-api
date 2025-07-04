insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, created_time, created_by, paid)
values (1, 'MDI', 1, 1, true, false, false, false, 'H', 'Maths', 'Maths Level 1', '2022-10-10', null, 'high', '2022-9-21 00:00:00', 'SEED USER', true);
insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, created_time, created_by, paid)
values (2, 'MDI', 1, 1, true, false, false, false, 'H', 'Maths', 'Maths Level 2', '2022-11-10', null, 'high', '2022-9-21 00:00:00', 'SEED USER', true);
insert into activity(activity_id, prison_code, activity_category_id, activity_tier_id, attendance_required, in_cell, piece_work, outside_work, pay_per_session, summary, description, start_date, end_date, risk_level, created_time, created_by, paid)
values (3, 'MDI', 1, 1, true, false, false, false, 'H', 'Maths', 'Maths Level 3', '2022-12-10', null, 'high', '2022-9-21 00:00:00', 'SEED USER', true);


insert into activity_pay(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items, start_date)
values (1, 1, 'BAS', 'Basic', 11, 100, 100, 1, '2025-06-12');
insert into activity_pay(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items, start_date)
values (2, 1, 'STD', 'Standard', 11, 150, 150, 1, '2025-06-15');
insert into activity_pay(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
values (3, 1, 'BAS', 'Basic', 11, 150, 150, 1);
insert into activity_pay(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items, start_date)
values (4, 1, 'BAS', 'Basic', 11, 75, 75, 1, '2025-06-06');
insert into activity_pay(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
values (5, 1, 'STD', 'Standard', 11, 50, 50, 1);
insert into activity_pay(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items, start_date)
values (6, 1, 'STD', 'Standard', 11, 200, 200, 1, '2025-05-06');
insert into activity_pay(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
values (7, 2, 'BAS', 'Basic', 13, 125, 125, 1);
insert into activity_pay(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
values (8, 2, 'STD', 'Standard', 14, 100, 100, 1);
insert into activity_pay(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
values (9, 2, 'STD', 'Standard', 11, 150, 150, 1);
insert into activity_pay(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items, start_date)
values (10, 2, 'STD', 'Standard', 14, 125, 125, 1, '2025-06-11');
insert into activity_pay(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items, start_date)
values (11, 2, 'BAS', 'Basic', 13, 85, 85, 1, '2025-05-01');
insert into activity_pay(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items, start_date)
values (12, 1, 'STD', 'Standard', 11, 95, 95, 1, '2025-04-29');
insert into activity_pay(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
values (13, 1, 'BAS', 'Basic', 13, 55, 55, 1);
insert into activity_pay(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
values (14, 1, 'STD', 'Standard', 13, 70, 70, 1);
insert into activity_pay(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items, start_date)
values (15, 2, 'STD', 'Standard', 14, 200, 200, 1, '2025-04-07');
insert into activity_pay(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
values (16, 3, 'STD', 'Standard', 11, 75, 75, 1);
insert into activity_pay(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
values (17, 2, 'BAS', 'Basic', 11, 125, 125, 1);
insert into activity_pay(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
values (18, 2, 'STD', 'Standard', 13, 125, 125, 1);
insert into activity_pay(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items)
values (19, 3, 'BAS', 'Basic', 11, 150, 150, 1);
insert into activity_pay(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items, start_date)
values (20, 1, 'BAS', 'Basic', 11, 125, 125, 1, '2025-01-10');
insert into activity_pay(activity_pay_id, activity_id, incentive_nomis_code, incentive_level, prison_pay_band_id, rate, piece_rate, piece_rate_items, start_date)
values (21, 3, 'BAS', 'Basic', 11, 125, 125, 1, '2025-04-22');