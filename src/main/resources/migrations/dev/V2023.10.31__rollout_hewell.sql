SELECT setval('rollout_prison_rollout_prison_id_seq', (SELECT MAX(rollout_prison_id) FROM rollout_prison), true);
insert into rollout_prison (code, description,
                            activities_to_be_rolled_out, activities_rollout_date,
                            appointments_to_be_rolled_out, appointments_rollout_date)
values ('HEI', 'HMP Hewell', true, '2023-10-31', true, '2023-10-31');

SELECT setval('prison_regime_prison_regime_id_seq', (SELECT MAX(prison_regime_id) FROM prison_regime), true);
insert into prison_regime (prison_code, am_start, am_finish, pm_start, pm_finish, ed_start, ed_finish)
VALUES ('HEI', '08:30:00', '11:45:00', '13:45:00', '16:45:00', '17:30:00', '19:15:00');

SELECT setval('prison_pay_band_prison_pay_band_id_seq', (SELECT MAX(prison_pay_band_id) FROM prison_pay_band), true);
insert into prison_pay_band (display_sequence, nomis_pay_band, pay_band_alias, pay_band_description, prison_code)
values (1, 1, 'Low', 'Pay band 1 (Lowest)', 'HEI'),
       (2, 2, 'Medium', 'Pay band 2', 'HEI'),
       (3, 3, 'High', 'Pay band 3 (highest)', 'HEI');
