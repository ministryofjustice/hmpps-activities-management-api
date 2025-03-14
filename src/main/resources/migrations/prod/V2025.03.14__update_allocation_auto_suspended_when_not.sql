update allocation a
set suspended_time = null, suspended_by = null, suspended_reason = null, prisoner_status = 'ACTIVE'
where a.allocation_id in (173847, 152470) and a.prisoner_status = 'AUTO_SUSPENDED';

