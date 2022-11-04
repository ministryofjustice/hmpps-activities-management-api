insert into activity_pay(activity_pay_id, activity_id, incentive_level, pay_band, rate, piece_rate, piece_rate_items)
values (1, 1, 'BAS', 'A', 125, 150, 10);

alter sequence activity_pay_activity_pay_id_seq restart with 2;