-- Setup HMP Isle of Wight for 26/07/2024
insert into rollout_prison (code, description, activities_to_be_rolled_out, activities_rollout_date, appointments_to_be_rolled_out, appointments_rollout_date)
values ('IWI', 'HMP Isle of Wight', false, '2024-07-26', false, '2024-07-26');

-- Regime times for HMP Isle of Wight
insert into prison_regime (prison_code, am_start, am_finish, pm_start, pm_finish, ed_start, ed_finish)
VALUES ('IWI', '08:25:00', '11:35:00', '13:40:00', '16:00:00', '18:00:00', '19:00:00');

-- Pay bands for HMP Isle of Wight
insert into prison_pay_band (display_sequence, nomis_pay_band, pay_band_alias, pay_band_description, prison_code)
values (1,  1,  'Pay band 1 (Lowest)', 'Pay band 1 (Lowest)', 'IWI'),
       (2,  2,  'Pay band 2', 'Pay band 2', 'IWI'),
       (3,  3,  'Pay band 3', 'Pay band 3', 'IWI'),
       (4,  4,  'Pay band 4', 'Pay band 4', 'IWI'),
       (5,  5,  'Pay band 5', 'Pay band 5', 'IWI'),
       (6,  6,  'Pay band 6', 'Pay band 6', 'IWI'),
       (7,  7,  'Pay band 7', 'Pay band 7', 'IWI'),
       (8,  8,  'Pay band 8', 'Pay band 8', 'IWI'),
       (9,  9,  'Pay band 9', 'Pay band 9', 'IWI'),
       (10, 10, 'Pay band 10 (Highest)', 'Pay band 10 (Highest)', 'IWI');
