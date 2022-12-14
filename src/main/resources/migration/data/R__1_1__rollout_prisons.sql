insert into rollout_prison (rollout_prison_id, code, description, active, rollout_date) select 1, 'LEI', 'HMP Leeds', true, '2022-12-19' where not exists (select 1 from rollout_prison where rollout_prison_id = 1);
insert into rollout_prison (rollout_prison_id, code, description, active, rollout_date) select 2, 'MDI', 'HMP Moorland', true, '2022-11-20' where not exists (select 1 from rollout_prison where rollout_prison_id = 2);
insert into rollout_prison (rollout_prison_id, code, description, active, rollout_date) select 3, 'PVI', 'HMP Pentonville', true, '2022-10-21' where not exists (select 1 from rollout_prison where rollout_prison_id = 3);

alter sequence rollout_prison_rollout_prison_id_seq restart with 4;