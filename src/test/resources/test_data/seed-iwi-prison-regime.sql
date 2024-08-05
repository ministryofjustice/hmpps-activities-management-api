insert into rollout_prison (rollout_prison_id, code, description, activities_to_be_rolled_out, activities_rollout_date, appointments_to_be_rolled_out, appointments_rollout_date)
values (100, 'IWI', 'HMP Isle of Wight', true, current_date, false, current_date);

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
