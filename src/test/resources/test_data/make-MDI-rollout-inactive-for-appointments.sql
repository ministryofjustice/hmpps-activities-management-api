update rollout_prison
set appointments_to_be_rolled_out = false,
    appointments_rollout_date     = null
where code = 'MDI';
