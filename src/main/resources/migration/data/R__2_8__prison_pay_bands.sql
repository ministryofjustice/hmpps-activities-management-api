insert into prison_pay_band (prison_pay_band_id, display_sequence, nomis_pay_band, pay_band_alias, pay_band_description, prison_code)
values (1, 1, 1, 'Pay band 1 (lowest)', 'Pay band 1 (lowest)', 'DEFAULT'),
       (2, 2, 2, 'Pay band 2', 'Pay band 2', 'DEFAULT'),
       (3, 3, 3, 'Pay band 3', 'Pay band 3', 'DEFAULT'),
       (4, 4, 4, 'Pay band 4', 'Pay band 4', 'DEFAULT'),
       (5, 5, 5, 'Pay band 5', 'Pay band 5', 'DEFAULT'),
       (6, 6, 6, 'Pay band 6', 'Pay band 6', 'DEFAULT'),
       (7, 7, 7, 'Pay band 7', 'Pay band 7', 'DEFAULT'),
       (8, 8, 8, 'Pay band 8', 'Pay band 8', 'DEFAULT'),
       (9, 9, 9, 'Pay band 9', 'Pay band 9', 'DEFAULT'),
       (10, 10, 10, 'Pay band 10 (highest)', 'Pay band 10 (highest)', 'DEFAULT'),
       (11, 1, 1, 'Low', 'Pay band 1', 'MDI'),
       (12, 2, 2, 'Medium', 'Pay band 2', 'MDI'),
       (13, 3, 3, 'High', 'Pay band 3', 'MDI');

alter sequence event_priority_event_priority_id_seq restart with 14;
