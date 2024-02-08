-- Setup HMP Wakefield for 16/02/2024
insert into rollout_prison (code, description, activities_to_be_rolled_out, activities_rollout_date, appointments_to_be_rolled_out, appointments_rollout_date)
values ('WDI', 'HMP Wakefield', false, '2024-02-16', false, '2024-02-16');

-- Regime times for HMP Wakefield
insert into prison_regime (prison_code, am_start, am_finish, pm_start, pm_finish, ed_start, ed_finish)
VALUES ('WDI', '08:15:00', '11:30:00', '13:45:00', '16:30:00', '18:30:00', '19:30:00');

-- Pay bands for HMP Wakefield
insert into prison_pay_band (display_sequence, nomis_pay_band, pay_band_alias, pay_band_description, prison_code)
values (1,  1,  'Pay band 1 (Lowest)', 'Pay band 1 (Lowest)', 'WDI'),
       (2,  2,  'Pay band 2', 'Pay band 2', 'WDI'),
       (3,  3,  'Pay band 3', 'Pay band 3', 'WDI'),
       (4,  4,  'Pay band 4', 'Pay band 4', 'WDI'),
       (5,  5,  'Pay band 5', 'Pay band 5', 'WDI'),
       (6,  6,  'Pay band 6', 'Pay band 6', 'WDI'),
       (7,  7,  'Pay band 7', 'Pay band 7', 'WDI'),
       (8,  8,  'Pay band 8', 'Pay band 8', 'WDI'),
       (9,  9,  'Pay band 9', 'Pay band 9', 'WDI'),
       (10, 10, 'Pay band 10 (Highest)', 'Pay band 10 (Highest)', 'WDI');