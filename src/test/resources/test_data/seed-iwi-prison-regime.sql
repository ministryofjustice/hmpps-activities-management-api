insert into rollout_prison (rollout_prison_id, code, description, activities_to_be_rolled_out, activities_rollout_date, appointments_to_be_rolled_out, appointments_rollout_date)
values (100, 'IWI', 'HMP Isle of Wight', true, current_date, false, current_date);

insert into prison_regime (prison_regime_id, prison_code, am_start, am_finish, pm_start, pm_finish, ed_start, ed_finish)
VALUES (100, 'IWI', '08:25:00', '11:35:00', '13:40:00', '16:00:00', '18:00:00', '19:00:00');