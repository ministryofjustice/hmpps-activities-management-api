-- Setup HMP Liverpool for 08/12/2023
insert into rollout_prison (code, description, activities_to_be_rolled_out, activities_rollout_date, appointments_to_be_rolled_out, appointments_rollout_date)
values ('LPI', 'HMP Liverpool', false, '2023-12-08', false, '2023-12-08');

-- Regime times for HMP Liverpool
insert into prison_regime (prison_code, am_start, am_finish, pm_start, pm_finish, ed_start, ed_finish)
VALUES ('LPI', '08:30:00', '11:30:00', '13:30:00', '16:30:00', '18:30:00', '19:30:00');

-- Pay bands for HMP Liverpool
insert into prison_pay_band (display_sequence, nomis_pay_band, pay_band_alias, pay_band_description, prison_code)
values (1,  1,  'Pay band 1 (Lowest)', 'Pay band 1 (Lowest)', 'LPI'),
       (2,  2,  'Pay band 2', 'Pay band 2', 'LPI'),
       (3,  3,  'Pay band 3', 'Pay band 3', 'LPI'),
       (4,  4,  'Pay band 4', 'Pay band 4', 'LPI'),
       (5,  5,  'Pay band 5', 'Pay band 5', 'LPI'),
       (6,  6,  'Pay band 6', 'Pay band 6', 'LPI'),
       (7,  7,  'Pay band 7', 'Pay band 7', 'LPI'),
       (8,  8,  'Pay band 8', 'Pay band 8', 'LPI'),
       (9,  9,  'Pay band 9', 'Pay band 9', 'LPI'),
       (10, 10, 'Pay band 10 (Highest)', 'Pay band 10 (Highest)', 'LPI');
