insert into rollout_prison (rollout_prison_id, code, description, activities_to_be_rolled_out, activities_rollout_date, appointments_to_be_rolled_out, appointments_rollout_date)
values (100, 'IWI', 'HMP Isle of Wight', true, current_date, false, current_date);

insert into prison_pay_band(prison_pay_band_id,display_sequence, nomis_pay_band, pay_band_alias, pay_band_description, prison_code)
values (10001,1, 1, 'Low', 'Pay band 1 (Lowest)', 'IWI'),
       (20001,2, 2, 'Medium', 'Pay band 2', 'IWI'),
       (30001,3, 3, 'High', 'Pay band 3 (highest)', 'IWI');

insert into prison_regime (prison_regime_id, prison_code, am_start, am_finish, pm_start, pm_finish, ed_start, ed_finish)
VALUES (100, 'IWI', '08:25:00', '11:35:00', '13:40:00', '16:50:00', '18:00:00', '19:00:00');

insert into prison_regime_days_of_week(prison_regime_id, day_of_week)
values (100, 'FRIDAY');

insert into prison_regime (prison_regime_id, prison_code, am_start, am_finish, pm_start, pm_finish, ed_start, ed_finish)
VALUES (101, 'IWI', '09:25:00', '11:35:00', '13:40:00', '16:50:00', '18:00:00', '19:00:00');

insert into prison_regime_days_of_week(prison_regime_id, day_of_week)
values (101, 'MONDAY'), (101, 'TUESDAY'), (101, 'WEDNESDAY'), (101, 'THURSDAY');

insert into prison_regime (prison_regime_id, prison_code, am_start, am_finish, pm_start, pm_finish, ed_start, ed_finish)
VALUES (102, 'IWI', '09:00:00', '11:30:00', '13:40:00', '16:45:00', '18:00:00', '19:00:00');

insert into prison_regime_days_of_week(prison_regime_id, day_of_week)
values (102, 'SATURDAY'), (102, 'SUNDAY');
