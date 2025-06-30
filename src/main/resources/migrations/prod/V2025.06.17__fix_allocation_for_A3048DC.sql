update allocation set
    prisoner_status = 'ACTIVE',
    suspended_time = null,
    suspended_by = null,
    suspended_reason = null
where
    allocation_id = 306230 and
    prisoner_number = 'A3048DC';