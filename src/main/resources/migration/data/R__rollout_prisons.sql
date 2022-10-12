insert into rollout_prison (rollout_prison_id, code, description, active) select 1, 'LEI', 'HMP Leeds', true where not exists (select 1 from rollout_prison where rollout_prison_id = 1);
insert into rollout_prison (rollout_prison_id, code, description, active) select 2, 'MDI', 'HMP Moorland', true where not exists (select 1 from rollout_prison where rollout_prison_id = 2);
insert into rollout_prison (rollout_prison_id, code, description, active) select 3, 'PVI', 'HMP Pentonville', true where not exists (select 1 from rollout_prison where rollout_prison_id = 3);
