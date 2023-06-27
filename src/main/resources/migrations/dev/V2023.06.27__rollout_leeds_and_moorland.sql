delete from rollout_prison where code = 'PVI';

update rollout_prison set activities_to_be_rolled_out = true where code = 'MDI';
update rollout_prison set activities_rollout_date = '2023-06-27' where code = 'MDI';
update rollout_prison set appointments_to_be_rolled_out = true where code = 'MDI';
update rollout_prison set appointments_rollout_date = '2023-06-27' where code = 'MDI';

update rollout_prison set activities_to_be_rolled_out = true where code = 'LEI';
update rollout_prison set activities_rollout_date = '2023-06-27' where code = 'LEI';
update rollout_prison set appointments_to_be_rolled_out = true where code = 'LEI';
update rollout_prison set appointments_rollout_date = '2023-06-27' where code = 'LEI';
