insert into activity_pay(activity_pay_id, activity_id, iep_basic_rate, iep_standard_rate, iep_enhanced_rate, piece_rate, piece_rate_items)
values (1, 1, 100, 125, 150, 150, 10);

alter sequence activity_pay_activity_pay_id_seq restart with 2;