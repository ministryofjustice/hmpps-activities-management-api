update rollout_prison
set activities_to_be_rolled_out   = false,
    activities_rollout_date       = null,
    appointments_to_be_rolled_out = false,
    appointments_rollout_date     = null
where code = 'MDI';
