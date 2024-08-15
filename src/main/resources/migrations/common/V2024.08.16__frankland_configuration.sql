-- Setup HMP Frankland, final dates tbc
insert into rollout_prison (code, description, activities_to_be_rolled_out, activities_rollout_date, appointments_to_be_rolled_out, appointments_rollout_date)
values ('FKI', 'HMP Frankland', false, '2024-09-24', false, '2024-08-16');

-- Regime times for HMP Frankland
insert into prison_regime (prison_code, am_start, am_finish, pm_start, pm_finish, ed_start, ed_finish)
VALUES ('FKI', '08:30:00', '11:45:00', '13:30:00', '17:00:00', '17:00:00', '19:00:00');

-- Pay bands for HMP Frankland
insert into prison_pay_band (display_sequence, nomis_pay_band, pay_band_alias, pay_band_description, prison_code)
values (1,  1,  'Pay band 1 (Lowest)', 'Pay band 1 (Lowest)', 'FKI'),
       (2,  2,  'Pay band 2', 'Pay band 2', 'FKI'),
       (3,  3,  'Pay band 3', 'Pay band 3', 'FKI'),
       (10, 10, 'Pay band 4 (Highest)', 'Pay band 4 (Highest)', 'FKI');
