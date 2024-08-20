-- Setup HMP Feltham, final dates tbc
insert into rollout_prison (code, description, activities_to_be_rolled_out, activities_rollout_date, appointments_to_be_rolled_out, appointments_rollout_date)
values ('FMI', 'HMP Feltham', false, '2024-09-24', false, '2024-09-24');

-- Regime times for HMP Feltham
insert into prison_regime (prison_code, am_start, am_finish, pm_start, pm_finish, ed_start, ed_finish)
VALUES ('FMI', '08:30:00', '11:30:00', '13:30:00', '16:30:00', '17:30:00', '18:30:00');

-- Regime days-of-week for Feltham (no variation between days)
insert into prison_regime_days_of_week(prison_regime_id, day_of_week)
values ((select prison_regime_id from prison_regime where prison_code = 'FMI'), 'MONDAY'),
((select prison_regime_id from prison_regime where prison_code = 'FMI'), 'TUESDAY'),
((select prison_regime_id from prison_regime where prison_code = 'FMI'), 'WEDNESDAY'),
((select prison_regime_id from prison_regime where prison_code = 'FMI'), 'THURSDAY'),
((select prison_regime_id from prison_regime where prison_code = 'FMI'), 'FRIDAY'),
((select prison_regime_id from prison_regime where prison_code = 'FMI'), 'SATURDAY'),
((select prison_regime_id from prison_regime where prison_code = 'FMI'), 'SUNDAY');

-- Pay bands for HMP Feltham
insert into prison_pay_band (display_sequence, nomis_pay_band, pay_band_alias, pay_band_description, prison_code)
values (1,  1,  'Pay band 1 (Lowest)', 'Pay band 1 (Lowest)', 'FMI'),
       (2, 2, 'Pay band 2 (Highest)', 'Pay band 2 (Highest)', 'FMI');
