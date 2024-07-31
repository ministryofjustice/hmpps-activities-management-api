insert into rollout_prison (rollout_prison_id, code, description, activities_to_be_rolled_out,
                            activities_rollout_date, appointments_to_be_rolled_out,
                            appointments_rollout_date)
values (3, 'RSI', 'HMP Risley', true, '2022-12-22', false, null);

insert into prison_regime (prison_code, am_start, am_finish, pm_start, pm_finish, ed_start, ed_finish)
VALUES ('RSI', '08:30:00', '11:45:00', '13:45:00', '16:45:00', '17:30:00', '19:15:00');
