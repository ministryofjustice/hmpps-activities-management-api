insert into rollout_prison (code, description,
                            activities_to_be_rolled_out, activities_rollout_date,
                            appointments_to_be_rolled_out, appointments_rollout_date)
values ('RSI', 'HMP Risley', true, '2023-07-20', true, '2023-07-20');

insert into prison_regime (prison_code, am_start, am_finish, pm_start, pm_finish, ed_start, ed_finish)
VALUES ('RSI', '08:30:00', '11:45:00', '13:45:00', '16:45:00', '17:30:00', '19:15:00');

insert into prison_pay_band (display_sequence, nomis_pay_band, pay_band_alias, pay_band_description, prison_code)
values (1, 1, 'Low', 'Pay band 1 (Lowest)', 'RSI'),
       (2, 2, 'Medium', 'Pay band 2', 'RSI'),
       (3, 3, 'High', 'Pay band 3 (highest)', 'RSI');
